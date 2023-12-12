package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, JsNumber, JsString, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.models.RegionExport
import uk.gov.homeoffice.drt.routes.LegacyExportRoutes.LegacyRegionExportRequest
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

object LegacyRegionExportJsonFormats extends DefaultJsonProtocol {

  implicit object LocalDateJsonFormat extends RootJsonFormat[LocalDate] {
    override def read(json: JsValue): LocalDate = json match {
      case JsString(s) => s.split("-") match {
        case Array(year, month, day) => LocalDate(year.toInt, month.toInt, day.toInt)
      }
    }

    override def write(obj: LocalDate): JsValue = JsString(obj.toString)
  }

  implicit object SDateLikeJsonFormat extends RootJsonFormat[SDateLike] {
    override def read(json: JsValue): SDateLike = json match {
      case JsNumber(ts) => SDate(ts.toLong)
    }

    override def write(obj: SDateLike): JsValue = obj.millisSinceEpoch.toJson
  }

  implicit val regionExportRequestJsonFormat: RootJsonFormat[LegacyRegionExportRequest] = jsonFormat3(LegacyRegionExportRequest.apply)
  implicit val regionExportJsonFormat: RootJsonFormat[RegionExport] = jsonFormat6(RegionExport.apply)
}
