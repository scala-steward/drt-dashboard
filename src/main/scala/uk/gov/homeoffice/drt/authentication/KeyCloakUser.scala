package uk.gov.homeoffice.drt.authentication

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

trait KeyCloakUserJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val keyCloakUserFormatParser: RootJsonFormat[KeyCloakUser] = jsonFormat7(KeyCloakUser)
}

case class KeyCloakUser(
  id: String,
  username: String,
  enabled: Boolean,
  emailVerified: Boolean,
  firstName: String,
  lastName: String,
  email: String)

case class KeyCloakGroup(
  id: String,
  name: String,
  path: String)

