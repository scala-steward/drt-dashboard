package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.Urls
import uk.gov.homeoffice.drt.authentication.User


case class IndexRoute(urls: Urls, indexResource: Route) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val route: Route =
    concat(
      pathPrefix("static") {
        getFromResourceDirectory("frontend/static")
      },
      pathPrefix("images") {
        getFromResourceDirectory("frontend/images")
      },
      pathPrefix("") {
        concat(
          pathEnd {
            respondWithHeaders(Seq(
              RawHeader("Cache-Control", "no-cache, no-store, must-revalidate"),
              RawHeader("Pragma", "no-cache"),
              RawHeader("Expires", "0"),
            ))(indexRouteDirectives)
          },
        )
      },
      pathPrefix("access-requests")(indexRouteDirectives),
      pathPrefix("users")(indexRouteDirectives),
      pathPrefix("alerts")(indexRouteDirectives),
      pathPrefix("region")(indexRouteDirectives),
      pathPrefix("feature-guides")(indexRouteDirectives),
      pathPrefix("drop-in-sessions")(indexRouteDirectives),
      pathPrefix("feedback")(indexRouteDirectives),
      pathPrefix("user-feedback")(indexRouteDirectives),
      pathPrefix("health-checks")(indexRouteDirectives),
      pathPrefix("download")(indexRouteDirectives),
      pathPrefix("export-config")(indexRouteDirectives),
    )

  def indexRouteDirectives: Route = {
    parameterMap { params =>
      optionalHeaderValueByName("X-Auth-Roles") { maybeRoles =>
        (params.get("fromPort").flatMap(urls.portCodeFromUrl), maybeRoles) match {
          case (Some(portCode), Some(rolesStr)) =>
            val user = User.fromRoles("", rolesStr)
            if (user.accessiblePorts.contains(portCode)) {
              val portLogoutUrl = urls.logoutUrlForPort(portCode)
              log.info(s"Redirecting back to $portCode ($portLogoutUrl)")
              redirect(portLogoutUrl, StatusCodes.TemporaryRedirect)
            } else {
              log.info(s"Redirecting to root url as originating $portCode is not available to user")
              redirect(urls.rootUrl, StatusCodes.TemporaryRedirect)
            }
          case _ =>
            log.info(s"Presenting application to user with roles ($maybeRoles)")
            indexResource
        }
      }
    }
  }
}
