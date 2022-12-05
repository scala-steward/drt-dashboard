package uk.gov.homeoffice.drt.authentication

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

case class AccessRequest(agreeDeclaration: Boolean,
                         allPorts: Boolean,
                         lineManager: String,
                         portOrRegionText: String,
                         portsRequested: Set[String],
                         rccOption: String,
                         regionsRequested: Set[String],
                         staffing: Boolean,
                         staffText: String,
                        )

trait AccessRequestJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val accessRequestFormatParser: RootJsonFormat[AccessRequest] = jsonFormat9(AccessRequest)
}
