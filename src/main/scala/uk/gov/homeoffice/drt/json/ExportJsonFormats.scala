package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}
import uk.gov.homeoffice.drt.exports._
import uk.gov.homeoffice.drt.json.LocalDateJsonFormats.LocalDateJsonFormat
import uk.gov.homeoffice.drt.json.SDateLikeJsonFormats.SDateLikeTimestampJsonFormat
import uk.gov.homeoffice.drt.models.Export
import uk.gov.homeoffice.drt.routes.ExportRoutes.ExportRequest

object ExportJsonFormats extends DefaultJsonProtocol {

  implicit object ExportTypeJsonFormat extends RootJsonFormat[ExportType] {
    override def read(json: JsValue): ExportType = json match {
      case JsString(exportTypeString) => ExportTypes.parse(exportTypeString)
    }

    override def write(obj: ExportType): JsValue = obj match {
      case Arrivals => JsString("arrivals")
      case PortPassengers => JsString("passengers-port")
      case TerminalPassengers => JsString("passengers-terminal")
      case PortPassengersDaily => JsString("passengers-port-daily")
      case TerminalPassengersDaily => JsString("passengers-terminal-daily")
    }
  }

  implicit val exportPortJsonFormat: RootJsonFormat[ExportPort] = jsonFormat2(ExportPort.apply)

  implicit val exportRequestJsonFormat: RootJsonFormat[ExportRequest] = jsonFormat4(ExportRequest.apply)

  implicit val exportJsonFormat: RootJsonFormat[Export] = jsonFormat6(uk.gov.homeoffice.drt.models.Export.apply)
}
