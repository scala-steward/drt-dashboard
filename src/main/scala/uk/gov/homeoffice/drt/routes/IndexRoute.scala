package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ concat, optionalHeaderValueByName, parameterMap, path, pathPrefix, redirect }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.routes.PortUrl.{ logoutUrlForPort, portCodeFromUrl }

object IndexRoute {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(indexResource: Route, directoryResource: Route, staticResourceDirectory: Route, domain: String): Route = {
    concat(
      path("") {
        indexRouteDirectives(indexResource, domain)
      },
      (get & pathPrefix("")) {
        directoryResource
      },
      (get & pathPrefix("static")) {
        staticResourceDirectory
      })
  }

  def indexRouteDirectives(directoryResource: Route, domain: String): Route = {
    parameterMap { params =>
      optionalHeaderValueByName("X-Auth-Roles") { maybeRoles =>
        (params.get("fromPort").flatMap(portCodeFromUrl), maybeRoles) match {
          case (Some(portCode), Some(rolesStr)) =>
            val user = User.fromRoles("", rolesStr)
            if (user.accessiblePorts.contains(portCode)) {
              val portLogoutUrl = logoutUrlForPort(portCode, domain)
              log.info(s"Redirecting back to $portCode ($portLogoutUrl)")
              redirect(portLogoutUrl, StatusCodes.TemporaryRedirect)
            } else {
              log.info(s"Presenting application to user with roles ($rolesStr). $portCode port not accessible. Accessible ports: ${user.accessiblePorts}")
              directoryResource
            }
          case _ =>
            log.info(s"Presenting application to user with roles ($maybeRoles)")
            directoryResource
        }
      }
    }
  }
}
