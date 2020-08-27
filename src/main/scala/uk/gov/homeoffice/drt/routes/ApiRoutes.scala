package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.server.Directives.{ complete, pathPrefix, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import spray.json.{ JsArray, JsObject, JsString }
import uk.gov.homeoffice.drt.authentication.{ Roles, User }

object ApiRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(prefix: String, portCodes: Array[String]): Route =
    pathPrefix(prefix) {
      import uk.gov.homeoffice.drt.authentication.UserJsonSupport._
      headerValueByName("X-Auth-Roles") { rolesStr =>
        headerValueByName("X-Auth-Email") { email =>
          concat(
            (get & path("user")) {
              complete(User.fromRoles(email, rolesStr))
            },
            (get & path("config")) {
              val json = JsObject(Map(
                "ports" -> JsArray(portCodes.map(JsString(_)).toVector),
                "domain" -> JsString("drt.homeoffice.gov.uk")))
              complete(json)
            })
        }
      }
      complete(User("ringo@albumsnaps.com", Set(Roles.LHR, Roles.BorderForceStaff)))
    }
}
