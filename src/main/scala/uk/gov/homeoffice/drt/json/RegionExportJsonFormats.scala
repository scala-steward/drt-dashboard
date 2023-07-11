package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, JsNumber, JsObject, JsString, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.models.RegionExport
import uk.gov.homeoffice.drt.routes.ExportRoutes.RegionExportRequest
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

    override def write(obj: SDateLike): JsValue = JsNumber(obj.millisSinceEpoch)
  }

  implicit object RegionExportJsonFormat extends RootJsonFormat[RegionExport] {
    override def read(json: JsValue): RegionExport = json match {
      case JsObject(fields) =>
        val email = fields("email").convertTo[String]
        val region = fields("region").convertTo[String]
        val startDate = fields("startDate").convertTo[LocalDate]
        val endDate = fields("endDate").convertTo[LocalDate]
        val status = fields("status").convertTo[String]
        val createdAt = fields("createdAt").convertTo[SDateLike]
        RegionExport(email, region, startDate, endDate, status, createdAt)
    }

    override def write(obj: RegionExport): JsValue = JsObject(Map(
      "email" -> obj.email.toJson,
      "region" -> obj.region.toJson,
      "startDate" -> obj.startDate.toJson,
      "endDate" -> obj.endDate.toJson,
      "status" -> obj.status.toJson,
      "createdAt" -> obj.createdAt.toJson,
    ))
  }

  implicit object RegionExportRequestJsonFormat extends RootJsonFormat[RegionExportRequest] {
    override def read(json: JsValue): RegionExportRequest = json match {
      case JsObject(fields) =>
        val region = fields("region").convertTo[String]
        val startDate = fields("startDate").convertTo[LocalDate]
        val endDate = fields("endDate").convertTo[LocalDate]
        RegionExportRequest(region, startDate, endDate)
    }

    override def write(obj: RegionExportRequest): JsValue = JsObject(Map(
      "region" -> obj.region.toJson,
      "startDate" -> obj.startDate.toJson,
      "endDate" -> obj.endDate.toJson,
    ))
  }
}
