package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import spray.json.enrichAny
import uk.gov.homeoffice.drt.auth.Roles.ManageUsers
import uk.gov.homeoffice.drt.authentication._
import uk.gov.homeoffice.drt.{ClientConfig, db}
import uk.gov.homeoffice.drt.db.UserRowJsonSupport
import uk.gov.homeoffice.drt.http.ProdSendAndReceive
import uk.gov.homeoffice.drt.keycloak.{KeycloakClient, KeycloakService}
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.routes.AlertsRoutes.clientUserAccessDataJsonSupportDataFormatParser
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.services.{UserRequestService, UserService}

import java.sql.Timestamp
import java.util.Date
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object UserRoutes extends db.UserAccessRequestJsonSupport
  with UserJsonSupport
  with UserRowJsonSupport
  with AccessRequestJsonSupport
  with KeyCloakUserJsonSupport {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(clientConfig: ClientConfig,
            userService: UserService,
            userRequestService: UserRequestService,
            notifications: EmailNotifications,
            keyClockUrl: String,
           )
           (implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route = {

    def getKeyCloakService(accessToken: String): KeycloakService = {
      val keyClockClient = new KeycloakClient(accessToken, keyClockUrl) with ProdSendAndReceive
      KeycloakService(keyClockClient)
    }

    concat(
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
                    log.info(s"request to get user details $keyClockUrl/data/userDetails/$userEmail}")
                    val keycloakService = getKeyCloakService(xAuthToken)
                    val keyCloakUser: Future[KeyCloakUser] =
                      keycloakService.getUserForEmail(userEmail).map {
                        case Some(keyCloakUser) => keyCloakUser
                        case None =>
                          log.error(s"Failed at $keyClockUrl/data/userDetails/$userEmail}")
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
                      val keycloakService = getKeyCloakService(xAuthToken)
                      if (userRequestedAccessData.portsRequested.nonEmpty || userRequestedAccessData.regionsRequested.nonEmpty) {
                        if (userRequestedAccessData.allPorts) {
                          keycloakService.addUserToGroup(id, "All Port Access")
                          if (userRequestedAccessData.accountType == "rccu") {
                            keycloakService.addUserToGroup(id, "All RCC Access")
                          }
                        } else {
                          Future.sequence(userRequestedAccessData.getListOfPortOrRegion.map { port =>
                            keycloakService.addUserToGroup(id, port)
                          })
                        }
                        keycloakService.addUserToGroup(id, "Border Force")
                        if (userRequestedAccessData.staffEditing) {
                          keycloakService.addUserToGroup(id, "Staff Admin")
                        }
                        userRequestService.updateUserRequest(userRequestedAccessData, "Approved")
                        notifications.sendAccessGranted(userRequestedAccessData, clientConfig.domain, clientConfig.teamEmail)
                        userService.upsertUser(
                          db.UserRow(id = userRequestedAccessData.email,
                            username = userRequestedAccessData.email,
                            email = userRequestedAccessData.email,
                            latest_login = new Timestamp(DateTime.now().getMillis),
                            inactive_email_sent = None,
                            revoked_access = None,
                            drop_in_notification_at = None,
                            created_at = Some(new Timestamp(DateTime.now().getMillis))), Some("Approved"))
                        complete(s"User ${userRequestedAccessData.email} update port ${userRequestedAccessData.portOrRegionText}")
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
    )
  }
}

