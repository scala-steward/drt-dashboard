package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.{ HttpHeader, StatusCodes }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{ complete, pathPrefix, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.User

object ApiRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def extractUser: HttpHeader => Option[User] = {
    case RawHeader("X-Auth-Roles", rolesString) => Option(User.fromRoles(rolesString))
    case _ => None
  }

  def apply(prefix: String, portCodes: Array[String]): Route = pathPrefix(prefix) {
    get {
      import uk.gov.homeoffice.drt.authentication.UserJsonSupport._
      optionalHeaderValue(extractUser) {
        case None =>
          complete(User.fromRoles("LHR"))
        case Some(user) if !user.hasStaffCredential || !user.hasPortAccess =>
          complete(user)
        case Some(user) =>
          complete(user)
      }
    }
  }
}
