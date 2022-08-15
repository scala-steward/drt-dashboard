package uk.gov.homeoffice.drt.authentication

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, JsArray, JsObject, JsString, JsValue, RootJsonFormat }
import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.{ PortAccess, Role, Staff }

case class User(email: String, roles: Set[Role]) {
  def hasStaffCredential: Boolean = roles.exists(_.isInstanceOf[Staff])

  def hasPortAccess: Boolean = roles.exists(_.isInstanceOf[PortAccess])

  def accessiblePorts: Set[String] = roles.filter(_.isInstanceOf[PortAccess]).map(_.name)

  def hasRole(role: Role): Boolean = roles.contains(role)
}

object User {
  def fromRoles(email: String, roles: String): User =
    User(email, roles.split(",").flatMap(Roles.parse).toSet)
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
      "ports" -> JsArray(user.accessiblePorts.map(JsString(_)).toVector),
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

case class AccessRequest(portsRequested: Set[String], allPorts: Boolean, rccuRegionsRequested: Set[String], staffing: Boolean, lineManager: String, agreeDeclaration: Boolean)

trait AccessRequestJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val AccessRequestFormatParser: RootJsonFormat[AccessRequest] = jsonFormat6(AccessRequest)
}
