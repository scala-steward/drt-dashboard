package uk.gov.homeoffice.drt.authentication

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

case class AccessRequest(
  portsRequested: Set[String],
  allPorts: Boolean,
  regionsRequested: Set[String],
  staffing: Boolean,
  lineManager: String,
  agreeDeclaration: Boolean,
  rccOption: String,
  portOrRegionText: String,
  staffText: String)

trait AccessRequestJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val accessRequestFormatParser: RootJsonFormat[AccessRequest] = jsonFormat9(AccessRequest)
}
