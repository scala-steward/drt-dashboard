package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ concat, optionalHeaderValueByName, pathPrefix, redirect }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.routes.PortUrl.{ logoutUrlForPort, portCodeFromUrl }

object IndexRoute {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(indexResource: Route, staticResourceDirectory: Route, domain: String): Route = {
    concat(
      (get & pathPrefix("")) {
        indexRouteDirectives(indexResource, domain)
      },
      (get & pathPrefix("static")) {
        staticResourceDirectory
      })
  }

  def indexRouteDirectives(indexResource: Route, domain: String): Route = {
    optionalHeaderValueByName("Referer") { maybeReferer =>
      optionalHeaderValueByName("X-Auth-Roles") { maybeRoles =>
        (maybeReferer.flatMap(portCodeFromUrl), maybeRoles) match {
          case (Some(portCode), Some(rolesStr)) =>
            val user = User.fromRoles("", rolesStr)
            if (user.accessiblePorts.contains(portCode))
              redirect(logoutUrlForPort(portCode, domain), StatusCodes.TemporaryRedirect)
            else {
              println(s"$portCode port not accessible $rolesStr, ${user.accessiblePorts}")
              indexResource
            }
          case _ =>
            println(s"$maybeReferer, $maybeRoles")
            indexResource
        }
      }
    }
  }
}
