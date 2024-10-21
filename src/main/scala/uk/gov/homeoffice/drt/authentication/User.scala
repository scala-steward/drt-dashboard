package uk.gov.homeoffice.drt.authentication

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, JsArray, JsObject, JsString, JsValue, RootJsonFormat}
import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.{PortAccess, Role}
import uk.gov.homeoffice.drt.ports.PortCode

case class User(email: String, roles: Set[Role]) {
  def accessiblePorts: Set[PortCode] = roles.filter(_.isInstanceOf[PortAccess]).map(role => PortCode(role.name))

  def hasRole(role: Role): Boolean = roles.contains(role)
}

object User {
  def fromRoles(email: String, roles: String): User = {
    val rolesSeq: Array[String] = roles.split(",").map(_.split("role:").last)
    User(email, rolesSeq.flatMap(Roles.parse).toSet)
  }
}

trait UserJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object RoleFormatParser extends RootJsonFormat[Role] {
    override def write(obj: Role): JsValue = JsObject(Map("name" -> JsString(obj.name)))

    override def read(json: JsValue): Role = json match {
      case str: JsString => Roles.parse(str.value).getOrElse(throw new Exception(s"Invalid role '${str.value}''"))
      case u => throw new Exception(s"Expected valid Role json: $u")
    }
  }

  implicit object UserFormatParser extends RootJsonFormat[User] {
    override def write(user: User): JsValue = JsObject(Map(
      "ports" -> JsArray(user.accessiblePorts.map(pc => JsString(pc.iata)).toVector),
      "roles" -> JsArray(user.roles.map(r => JsString(r.name)).toVector),
      "email" -> JsString(user.email)))

    override def read(json: JsValue): User = json match {
      case JsObject(fields) =>
        (fields.get("email"), fields.get("ports")) match {
          case (Some(JsString(email)), Some(JsArray(portsArr))) =>
            val set = portsArr.collect {
              case JsString(port) => Roles.parse(port)
            }.flatten.toSet
            User(email, set)
          case _ => throw new Exception("Expected email and ports fields")
        }
    }
  }
}

