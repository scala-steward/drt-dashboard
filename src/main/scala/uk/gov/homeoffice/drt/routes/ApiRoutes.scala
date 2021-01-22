package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, pathPrefix, _}
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.{Directive0, Route}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.{JsArray, JsObject, JsString, enrichAny}
import uk.gov.homeoffice.drt.Dashboard
import uk.gov.homeoffice.drt.alerts.{AlertClient, MultiPortAlert}
import uk.gov.homeoffice.drt.auth.Roles.{CreateAlerts, Role}
import uk.gov.homeoffice.drt.authentication.{AccessRequest, User}
import uk.gov.homeoffice.drt.notifications.EmailNotifications

import scala.compat.java8.OptionConverters._
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object ApiRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def authByRole(role: Role): Directive0 = authorize(ctx => {
    (for {
      rolesHeader <- ctx.request.getHeader("X-Auth-Roles").asScala
      emailHeader <- ctx.request.getHeader("X-Auth-Email").asScala
    } yield User.fromRoles(emailHeader.value(), rolesHeader.value())) match {
      case Some(user) => user.hasRole(role)
      case None => false
    }
  })

  def apply(prefix: String, portCodes: Array[String], domain: String, notifications: EmailNotifications)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route =
    pathPrefix(prefix) {
      concat(
        (get & path("user")) {
          headerValueByName("X-Auth-Roles") { rolesStr =>
            headerValueByName("X-Auth-Email") { email =>
              import uk.gov.homeoffice.drt.authentication.UserJsonSupport._

              complete(User.fromRoles(email, rolesStr))
            }
          }
        },
        (get & path("config")) {
          headerValueByName("X-Auth-Roles") { _ =>
            import uk.gov.homeoffice.drt.authentication.UserJsonSupport._
            val json = JsObject(Map(
              "ports" -> JsArray(portCodes.map(JsString(_)).toVector),
              "domain" -> JsString(domain)))
            complete(json)
          }
        },
        (post & path("request-access")) {
          headerValueByName("X-Auth-Email") { userEmail =>
            import uk.gov.homeoffice.drt.authentication.AccessRequestJsonSupport._

            entity(as[AccessRequest]) { accessRequest =>
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
        (post & path("alerts")) {
          authByRole(CreateAlerts) {
            import uk.gov.homeoffice.drt.alerts.MultiPortAlertJsonSupport._
            headerValueByName("X-Auth-Roles") { rolesStr =>
              headerValueByName("X-Auth-Email") { email =>
                entity(as[MultiPortAlert]) {
                  multiPortAlert =>
                    {
                      multiPortAlert.alertForPorts(portCodes.toList).map {
                        case (portCode, alert) =>
                          import uk.gov.homeoffice.drt.alerts.MultiPortAlertJsonSupport._

                          val user = User.fromRoles(email, rolesStr)
                          val json = alert.toJson
                          AlertClient.postWithRoles(Dashboard.drtUriForPortCode(portCode), json.toString(), user.roles)

                      }
                      complete(StatusCodes.OK)

                    }
                }
              }
            }
          }
        })
    }
}

