package uk.gov.homeoffice.drt.routes

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.StatusCodes.InternalServerError
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.stream.Materializer
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import spray.json.enrichAny
import uk.gov.homeoffice.drt.auth.Roles.ManageUsers
import uk.gov.homeoffice.drt.authentication._
import uk.gov.homeoffice.drt.db.UserRowJsonSupport
import uk.gov.homeoffice.drt.keycloak._
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.routes.AlertsRoutes.clientUserAccessDataJsonSupportDataFormatParser
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.services.{UserRequestService, UserService}
import uk.gov.homeoffice.drt.{ClientConfig, db}

import java.sql.Timestamp
import java.util.Date
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object UserRoutes extends db.UserAccessRequestJsonSupport
  with UserJsonSupport
  with UserRowJsonSupport
  with AccessRequestJsonSupport
  with KeyCloakUserParserProtocol
  with KeyCloakAuthTokenParserProtocol {
  val log: Logger = LoggerFactory.getLogger(getClass)

  private def userRowForApprovedRequest(userRequestedAccessData: ClientUserRequestedAccessData): db.UserRow =
    db.UserRow(
      id = userRequestedAccessData.email,
      username = userRequestedAccessData.email,
      email = userRequestedAccessData.email,
      latest_login = new Timestamp(DateTime.now().getMillis),
      inactive_email_sent = None,
      revoked_access = None,
      drop_in_notification_at = None,
      created_at = Some(new Timestamp(DateTime.now().getMillis)),
    )

  private def groupsToAdd(userRequestedAccessData: ClientUserRequestedAccessData): Seq[String] = {
    val requestedGroups =
      if (userRequestedAccessData.allPorts) {
        val allPortsGroups = Seq("All Port Access")
        if (userRequestedAccessData.accountType == "rccu") allPortsGroups :+ "All RCC Access" else allPortsGroups
      } else {
        userRequestedAccessData.getListOfPortOrRegion
      }

    val staffGroups = if (userRequestedAccessData.staffEditing) Seq("Staff Admin") else Seq.empty

    (requestedGroups ++ Seq("Border Force") ++ staffGroups).distinct
  }

  private def grantUserAccess(id: String,
                              userRequestedAccessData: ClientUserRequestedAccessData,
                              keycloakService: IKeycloakService,
                              userRequestService: UserRequestService,
                              userService: UserService,
                              notifications: EmailNotifications,
                              clientConfig: ClientConfig,
                             )
                             (implicit ec: ExecutionContextExecutor): Future[String] = {
    val addGroups = Future.sequence(groupsToAdd(userRequestedAccessData).map(group => keycloakService.addUserToGroup(id, group)))

    addGroups.flatMap { _ =>
      for {
        _ <- userRequestService.updateUserRequest(userRequestedAccessData, "Approved")
        _ <- userService.upsertUser(userRowForApprovedRequest(userRequestedAccessData), Some("Approved"))
      } yield {
        notifications.sendAccessGranted(userRequestedAccessData, clientConfig.domain, clientConfig.teamEmail)
        s"User ${userRequestedAccessData.email} update port ${userRequestedAccessData.portOrRegionText}"
      }
    }
  }

  def apply(clientConfig: ClientConfig,
            userService: UserService,
            userRequestService: UserRequestService,
            notifications: EmailNotifications,
            keyCloakUrl: String,
           )
           (implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route = {

    implicit val mat: Materializer = Materializer.matFromSystem(system.classicSystem)
    val sendHttpRequest: HttpRequest => Future[HttpResponse] = request => Http().singleRequest(request)

    def getKeyCloakService(accessToken: String): IKeycloakService = {
      val keyClockClient = KeyCloakClient(accessToken, keyCloakUrl, sendHttpRequest)
      KeycloakService(keyClockClient)
    }

    apply(clientConfig, userService, userRequestService, notifications, keyCloakUrl, getKeyCloakService)
  }

  def apply(clientConfig: ClientConfig,
            userService: UserService,
            userRequestService: UserRequestService,
            notifications: EmailNotifications,
            keyCloakUrl: String,
            keyCloakServiceForToken: String => IKeycloakService,
           )
           (implicit ec: ExecutionContextExecutor): Route = {

    concat(
      (get & path("user")) {
        headerValueByName("X-Forwarded-Groups") { rolesStr =>
          headerValueByName("X-Forwarded-Email") { email =>
            complete(User.fromRoles(email, rolesStr))
          }
        }
      },
      (get & path("track-user")) {
        headerValueByName("X-Forwarded-Email") { email =>
          optionalHeaderValueByName("X-Forwarded-Preferred-Username") { usernameOption =>
            onComplete(
              userService.upsertUser(
                uk.gov.homeoffice.drt.db.UserRow(
                  id = usernameOption.getOrElse(email),
                  username = usernameOption.getOrElse(email),
                  email = email,
                  drop_in_notification_at = None,
                  latest_login = new Timestamp(new Date().getTime),
                  inactive_email_sent = None,
                  revoked_access = None,
                  created_at = Some(new Timestamp(new Date().getTime))
                ),
                Some("userTracking")
              )) {
              case Success(_) => complete(StatusCodes.OK)
              case Failure(ex) =>
                log.error(s"Failed to track user $email", ex)
                complete(InternalServerError)
            }
          }
        }
      },
      pathPrefix("users") {
        concat(
          (post & path("access-request")) {
            headerValueByName("X-Forwarded-Email") { userEmail =>
              entity(as[AccessRequest]) { accessRequest =>
                userRequestService.saveUserRequest(userEmail, accessRequest, new Timestamp(DateTime.now().getMillis))
                val failures = notifications.sendRequest(userEmail, accessRequest).foldLeft(List[(String, Throwable)]()) {
                  case (exceptions, (_, Success(_))) => exceptions
                  case (exceptions, (requestAddress, Failure(newException))) => (requestAddress, newException) :: exceptions
                }
                if (failures.nonEmpty) {
                  failures.foreach {
                    case (failedEmail, exception) =>
                      log.error(s"Failed to send access request email to $failedEmail", exception)
                  }
                  complete(StatusCodes.InternalServerError)
                } else complete(StatusCodes.OK)
              }
            }
          },
          (get & path("access-request")) {
            parameters("status") { status =>
              headerValueByName("X-Forwarded-Groups") { _ =>
                onComplete(userRequestService.getUserRequest(status)) {
                  case Success(value) => complete(value.toJson)
                  case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
                }
              }
            }
          },
          (get & path("all")) {
            headerValueByName("X-Forwarded-Groups") { _ =>
              onComplete(userService.getUsers()) {
                case Success(value) => complete(value.toJson)
                case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
              }
            }
          },
          (get & path("user-details" / Segment)) { userEmail =>
            AuthByRole(ManageUsers) {
              headerValueByName("X-Forwarded-Groups") { _ =>
                headerValueByName("X-Forwarded-Email") { _ =>
                  headerValueByName("X-Forwarded-Access-Token") { xAuthToken =>
                    log.info(s"request to get user details $keyCloakUrl/data/userDetails/$userEmail}")
                    val keycloakService = keyCloakServiceForToken(xAuthToken)
                    val keyCloakUser: Future[KeyCloakUser] =
                      keycloakService.getUserForEmail(userEmail).map {
                        case Some(keyCloakUser) => keyCloakUser
                        case None =>
                          log.error(s"Failed at $keyCloakUrl/data/userDetails/$userEmail}")
                          KeyCloakUser("", "", enabled = false, emailVerified = false, "", "", "")
                      }
                    complete(keyCloakUser)
                  }
                }
              }
            }
          },
          (post & path("accept-access-request" / Segment)) { id =>
            AuthByRole(ManageUsers) {
              headerValueByName("X-Forwarded-Groups") { _ =>
                headerValueByName("X-Forwarded-Email") { _ =>
                  headerValueByName("X-Forwarded-Access-Token") { xAuthToken =>
                    entity(as[ClientUserRequestedAccessData]) { userRequestedAccessData =>
                      val keycloakService = keyCloakServiceForToken(xAuthToken)
                      if (userRequestedAccessData.allPorts || userRequestedAccessData.portsRequested.nonEmpty || userRequestedAccessData.regionsRequested.nonEmpty) {
                        onComplete(grantUserAccess(
                          id,
                          userRequestedAccessData,
                          keycloakService,
                          userRequestService,
                          userService,
                          notifications,
                          clientConfig,
                        )) {
                          case Success(value) => complete(value)
                          case Failure(ex) =>
                            log.error(s"Failed to approve access request for ${userRequestedAccessData.email}", ex)
                            complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
                        }
                      } else {
                        complete("No port or region requested")
                      }
                    }
                  }
                }
              }
            }
          },
          (post & path("update-access-request" / Segment)) { status =>
            AuthByRole(ManageUsers) {
              headerValueByName("X-Forwarded-Groups") { _ =>
                headerValueByName("X-Forwarded-Email") { _ =>
                  entity(as[ClientUserRequestedAccessData]) { userRequestedAccessData =>
                    onComplete(userRequestService.updateUserRequest(userRequestedAccessData, status)) {
                      case Success(value) => complete(s"The result was $value")
                      case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
                    }
                  }
                }
              }
            }
          }
        )
      },
    )
  }
}
