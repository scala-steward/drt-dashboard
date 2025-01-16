package uk.gov.homeoffice.drt.routes.services

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.authorize
import uk.gov.homeoffice.drt.auth.Roles.Role
import uk.gov.homeoffice.drt.authentication.User

import scala.jdk.OptionConverters.RichOptional

object AuthByRole {
  def apply(role: Role): Directive0 = authorize(ctx => {
    (for {
      rolesHeader <- ctx.request.getHeader("X-Forwarded-Groups").toScala
      emailHeader <- ctx.request.getHeader("X-Forwarded-Email").toScala
    } yield User.fromRoles(emailHeader.value(), rolesHeader.value())) match {
      case Some(user) => user.hasRole(role)
      case None => false
    }
  })
}
