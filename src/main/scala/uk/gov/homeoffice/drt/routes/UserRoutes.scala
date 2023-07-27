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
import uk.gov.homeoffice.drt.db.{UserAccessRequestJsonSupport, UserJsonSupport}
import uk.gov.homeoffice.drt.http.ProdSendAndReceive
import uk.gov.homeoffice.drt.keycloak.{KeycloakClient, KeycloakService}
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.routes.ApiRoutes.{authByRole, clientUserAccessDataJsonSupportDataFormatParser}
import uk.gov.homeoffice.drt.services.{UserRequestService, UserService}
import uk.gov.homeoffice.drt.{ClientConfig, JsonSupport}

import java.sql.Timestamp
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object UserRoutes extends JsonSupport
  with UserAccessRequestJsonSupport
  with UserJsonSupport
  with AccessRequestJsonSupport
  with KeyCloakUserJsonSupport {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(
             prefix: String,
             clientConfig: ClientConfig,
             userService: UserService,
             userRequestService: UserRequestService,
             notifications: EmailNotifications,
             keyClockUrl: String)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route = {

    def getKeyCloakService(accessToken: String): KeycloakService = {
      val keyClockClient = new KeycloakClient(accessToken, keyClockUrl) with ProdSendAndReceive
      new KeycloakService(keyClockClient)
    }

    pathPrefix(prefix) {
      concat(
        (post & path("access-request")) {
          headerValueByName("X-Auth-Email") { userEmail =>
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
            headerValueByName("X-Auth-Roles") { _ =>
              onComplete(userRequestService.getUserRequest(status)) {
                case Success(value) =>
                  complete(value.toJson)
                case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
              }
            }
          }
        },
        (get & path("all")) {
          headerValueByName("X-Auth-Roles") { _ =>
            onComplete(userService.getUsers()) {
              case Success(value) =>
                complete(value.toJson)
              case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
            }
          }
        },
        (get & path("user-details" / Segment)) { userEmail =>
          authByRole(ManageUsers) {
            headerValueByName("X-Auth-Roles") { _ =>
              headerValueByName("X-Auth-Email") { _ =>
                headerValueByName("X-Auth-Token") { xAuthToken =>
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
          authByRole(ManageUsers) {
            headerValueByName("X-Auth-Roles") { _ =>
              headerValueByName("X-Auth-Email") { _ =>
                headerValueByName("X-Auth-Token") { xAuthToken =>
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
          authByRole(ManageUsers) {
            headerValueByName("X-Auth-Roles") { _ =>
              headerValueByName("X-Auth-Email") { _ =>
                entity(as[ClientUserRequestedAccessData]) { userRequestedAccessData =>
                  onComplete(userRequestService.updateUserRequest(userRequestedAccessData, status)) {
                    case Success(value) => complete(s"The result was $value")
                    case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
                  }
                }
              }
            }
          }
        })
    }
  }
}

