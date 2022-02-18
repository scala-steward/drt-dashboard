package uk.gov.homeoffice.drt

import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.ports.PortRegion

case class ClientConfig(portsByRegion: Iterable[PortRegion], domain: String, teamEmail: String)

trait ClientConfigJsonFormats extends DefaultJsonProtocol {
  implicit object PortRegionJsonFormat extends RootJsonFormat[PortRegion] {
    override def read(json: JsValue): PortRegion = throw new Exception("PortRegion deserialisation not yet implemented")

    override def write(obj: PortRegion): JsValue = JsObject(Map(
      "name" -> obj.name.toJson,
      "ports" -> obj.ports.map(_.iata).toJson,
    )
    )
  }

  implicit val clientConfigJsonFormat: RootJsonFormat[ClientConfig] = jsonFormat3(ClientConfig.apply)
}
