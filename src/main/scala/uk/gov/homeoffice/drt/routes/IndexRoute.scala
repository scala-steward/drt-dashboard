package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{ concat, optionalHeaderValueByName, parameterMap, path, pathPrefix, redirect }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.Urls
import uk.gov.homeoffice.drt.auth.Roles.NeboUpload
import uk.gov.homeoffice.drt.authentication.User

trait PathString

case object Root extends PathString

case object Alert extends PathString

case object Upload extends PathString

case class IndexRoute(urls: Urls, indexResource: Route, directoryResource: Route, staticResourceDirectory: Route) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val route: Route =
    concat(
      path("") {
        indexRouteDirectives(Root)
      },
      path("alerts") {
        indexRouteDirectives(Alert)
      },
      path("upload") {
        indexRouteDirectives(Upload)
      },
      (get & pathPrefix("")) {
        directoryResource
      },
      (get & pathPrefix("static")) {
        staticResourceDirectory
      })

  def indexRouteDirectives(pathString: PathString): Route = {
    parameterMap { params =>
      optionalHeaderValueByName("X-Auth-Roles") { maybeRoles =>
        (params.get("fromPort").flatMap(urls.portCodeFromUrl), maybeRoles) match {
          case (_, Some(rolesStr)) if rolesStr == NeboUpload.name && pathString != Upload =>
            log.info(s"Redirecting back to upload")
            redirect("upload", StatusCodes.TemporaryRedirect)
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
