package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ HttpHeader, StatusCodes }
import akka.http.scaladsl.server.Directives.{ complete, pathPrefix, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.{ Roles, User }

object LoginRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def extractUser: HttpHeader => Option[User] = {
    case RawHeader("x-auth-roles", rolesString) => Option(User(rolesString))
    case _ => None
  }

  def apply(prefix: String, portCodes: Array[String]): Route = pathPrefix(prefix) {
    get {
      optionalHeaderValue(extractUser) {
        case None =>
          complete("Access denied")
        case Some(user) if !user.hasStaffCredential =>
          complete("Please request access to DRT")
        case Some(user) if !user.hasPortAccess =>
          complete("Please request access to a DRT port")
        case Some(user) =>
          parameter("port".optional) {
            case None =>
              complete("Choose a port")
            case Some(portCode: String) =>
              Roles.parse(portCode) match {
                case None =>
                  complete("Invalid port")
                case Some(portRole) if user.hasRole(portRole) =>
                  redirect(s"$portCode", StatusCodes.TemporaryRedirect)
                case _ =>
                  complete(s"Please request access to $portCode")
              }
          }
      }
    }
  }
}
