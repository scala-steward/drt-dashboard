package uk.gov.homeoffice.drt.authentication

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, JsArray, JsBoolean, JsObject, JsString, JsValue, RootJsonFormat }
import uk.gov.homeoffice.drt.authentication.Roles.{ BorderForceStaff, PortAccess, Role, Staff }

case class User(roles: Set[Role]) {
  def hasStaffCredential: Boolean = roles.exists(_.isInstanceOf[Staff])

  def hasPortAccess: Boolean = roles.exists(_.isInstanceOf[PortAccess])

  def accessiblePorts: Set[String] = roles.filter(_.isInstanceOf[PortAccess]).map(_.name)

  def hasRole(role: Role): Boolean = roles.contains(role)
}

object User {
  def fromRoles(roles: String): User = {
    User(roles.split(",").flatMap(Roles.parse).toSet)
  }
}

object UserJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object RoleFormatParser extends RootJsonFormat[Role] {
    override def write(obj: Role): JsValue = JsObject(Map("name" -> JsString(obj.name)))

    override def read(json: JsValue): Role = json match {
      case str: JsString => Roles.parse(str.value).getOrElse(throw new Exception(s"Invalid role '${str.value}''"))
      case u => throw new Exception(s"Expected valid Role json: $u")
    }
  }

  implicit object UserFormatParser extends RootJsonFormat[User] {
    override def write(user: User): JsValue = JsObject(Map(
      "ports" -> JsArray(user.accessiblePorts.map(JsString(_)).toVector),
      "isPoise" -> JsBoolean(user.roles.contains(BorderForceStaff))))

    override def read(json: JsValue): User = ???
  }

}
