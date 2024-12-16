package uk.gov.homeoffice.drt.routes.services

import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.authorize
import uk.gov.homeoffice.drt.auth.Roles.Role
import uk.gov.homeoffice.drt.authentication.User

import scala.compat.java8.OptionConverters._

object AuthByRole {
  def apply(role: Role): Directive0 = authorize(ctx => {
    (for {
      rolesHeader <- ctx.request.getHeader("X-Forwarded-Groups").asScala
      emailHeader <- ctx.request.getHeader("X-Forwarded-Email").asScala
    } yield User.fromRoles(emailHeader.value(), rolesHeader.value())) match {
      case Some(user) => user.hasRole(role)
      case None => false
    }
  })
}
