package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, JsNumber, JsString, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.models.RegionExport
import uk.gov.homeoffice.drt.routes.LegacyExportRoutes.RegionExportRequest
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

object RegionExportJsonFormats extends DefaultJsonProtocol {

  implicit object LocalDateJsonFormat extends RootJsonFormat[LocalDate] {
    override def read(json: JsValue): LocalDate = json match {
      case JsString(s) => SDate(s).toLocalDate
    }

    override def write(obj: LocalDate): JsValue = JsString(obj.toString)
  }

  implicit object SDateLikeJsonFormat extends RootJsonFormat[SDateLike] {
    override def read(json: JsValue): SDateLike = json match {
      case JsNumber(ts) => SDate(ts.toLong)
    }

    override def write(obj: SDateLike): JsValue = obj.millisSinceEpoch.toJson
  }

  implicit val regionExportRequestJsonFormat: RootJsonFormat[RegionExportRequest] = jsonFormat3(RegionExportRequest.apply)
  implicit val regionExportJsonFormat: RootJsonFormat[RegionExport] = jsonFormat6(RegionExport.apply)
}
