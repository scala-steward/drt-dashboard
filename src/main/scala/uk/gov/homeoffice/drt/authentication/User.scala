package uk.gov.homeoffice.drt.authentication

import uk.gov.homeoffice.drt.authentication.Roles.{ PortAccess, Role, Staff }

case class User(roles: Set[Role]) {
  lazy val hasStaffCredential: Boolean = roles.exists(_.isInstanceOf[Staff])

  lazy val hasPortAccess: Boolean = roles.exists(_.isInstanceOf[PortAccess])

  lazy val accessiblePorts: Set[String] = roles.filter(_.isInstanceOf[PortAccess]).map(_.name)

  def hasRole(role: Role): Boolean = roles.contains(role)
}

object User {
  def apply(roles: String): User = {
    User(roles.split(",").flatMap(Roles.parse).toSet)
  }
}
