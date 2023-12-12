package uk.gov.homeoffice.drt

import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}

case class ClientConfig(portsByRegion: Iterable[PortRegion], ports: Map[PortCode, Seq[Terminal]], domain: String, teamEmail: String)

trait ClientConfigJsonFormats extends DefaultJsonProtocol {
  implicit object PortRegionJsonFormat extends RootJsonFormat[PortRegion] {
    override def read(json: JsValue): PortRegion = throw new Exception("PortRegion deserialisation not yet implemented")

    override def write(obj: PortRegion): JsValue = JsObject(Map(
      "name" -> obj.name.toJson,
      "ports" -> obj.ports.map(_.iata).toJson,
    ))
  }

  implicit object ClientConfigJsonFormat extends RootJsonFormat[ClientConfig] {
    override def read(json: JsValue): ClientConfig = throw new Exception("ClientConfig deserialisation not yet implemented")

    override def write(obj: ClientConfig): JsValue = JsObject(Map(
      "portsByRegion" -> obj.portsByRegion.toJson,
      "ports" -> obj.ports.map {
        case (portCode, terminals) => JsObject(Map(
          "iata" -> portCode.iata.toJson,
          "terminals" -> terminals.map(_.toString).toJson
        ))
      }.toJson,
      "domain" -> obj.domain.toJson,
      "teamEmail" -> obj.teamEmail.toJson,
    ))
  }
}
