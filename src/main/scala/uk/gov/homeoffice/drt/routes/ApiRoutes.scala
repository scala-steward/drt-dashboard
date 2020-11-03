package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ complete, pathPrefix, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import spray.json.{ JsArray, JsObject, JsString }
import uk.gov.homeoffice.drt.authentication.{ AccessRequest, User }
import uk.gov.homeoffice.drt.notifications.EmailNotifications

import scala.util.{ Failure, Success }

object ApiRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(prefix: String, portCodes: Array[String], domain: String, notifications: EmailNotifications): Route =
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
        })
    }
}
