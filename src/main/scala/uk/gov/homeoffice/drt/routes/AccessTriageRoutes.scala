package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{ complete, pathPrefix, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.routes.AccessTriageComponents.{ SimpleComponent, UserComponent }

object AccessTriageComponents {
  type SimpleComponent = () => String
  type UserComponent = User => String

  val externalUser: SimpleComponent = () => s"<h1>Welcome to DRT</h1><p>Please use the following form to request a DRT user account</p>"
  val internalUser: UserComponent = (user: User) => s"<h1>Welcome to DRT</h1><p>Please use the following form to request a DRT user account</p>"
  val existingUser: UserComponent = (user: User) => s"<h1>Welcome to DRT</h1><p>Where would you like to go? <ul>${user.accessiblePorts.map(p => s"""<li><a href="">${p.toUpperCase}</a></li>""")}</ul></p>"
}

object AccessTriageRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def extractUser: HttpHeader => Option[User] = {
    case RawHeader("X-Auth-Roles", rolesString) => Option(User.fromRoles(rolesString))
    case _ => None
  }

  def apply(externalUserComponent: SimpleComponent, internalUserComponent: UserComponent, existingUserComponent: UserComponent)(prefix: String, portCodes: Array[String]): Route = pathPrefix(prefix) {
    get {
      optionalHeaderValue(extractUser) {
        case None =>
          complete(externalUserComponent())
        case Some(user) if !user.hasStaffCredential || !user.hasPortAccess =>
          complete(internalUserComponent(user))
        case Some(user) =>
          complete(existingUserComponent(user))
      }
    }
  }
}
