package uk.gov.homeoffice.drt.services.api.v1.serialiser

import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.routes.api.v1.FlightApiV1Routes.{FlightJson, FlightJsonResponse}
import uk.gov.homeoffice.drt.time.SDate

trait FlightApiV1JsonFormats extends DefaultJsonProtocol with CommonJsonFormats {
  implicit object FlightJsonJsonFormat extends RootJsonFormat[FlightJson] {
    override def write(obj: FlightJson): JsValue = {
      val maybePax = obj.estimatedPaxCount.filter(_ > 0)
      JsObject(
        "arrivalPortCode" -> obj.arrivalPortCode.toJson,
        "arrivalTerminal" -> obj.arrivalTerminal.toJson,
        "code" -> obj.code.toJson,
        "originPortIata" -> obj.originPortIata.toJson,
        "originPortName" -> obj.originPortName.toJson,
        "scheduledTime" -> SDate(obj.scheduledTime).toISOString.toJson,
        "estimatedLandingTime" -> obj.estimatedLandingTime.map(SDate(_).toISOString).toJson,
        "actualChocksTime" -> obj.actualChocksTime.map(SDate(_).toISOString).toJson,
        "estimatedPcpStartTime" -> maybePax.flatMap(_ => obj.estimatedPcpStartTime.map(SDate(_).toISOString)).toJson,
        "estimatedPcpEndTime" -> maybePax.flatMap(_ => obj.estimatedPcpEndTime.map(SDate(_).toISOString)).toJson,
        "estimatedPcpPaxCount" -> obj.estimatedPaxCount.toJson,
        "status" -> obj.status.toJson
      )
    }

    override def read(json: JsValue): FlightJson = json match {
      case JsObject(fields) => FlightJson(
        fields.get("arrivalPortCode").map(_.convertTo[String]).getOrElse(""),
        fields.get("arrivalTerminal").map(_.convertTo[String]).getOrElse(""),
        fields.get("code").map(_.convertTo[String]).getOrElse(""),
        fields.get("originPortIata").map(_.convertTo[String]).getOrElse(""),
        fields.get("originPortName").map(_.convertTo[String]).getOrElse(""),
        fields.get("scheduledTime").map(_.convertTo[Long]).getOrElse(0L),
        fields.get("estimatedLandingTime").map(_.convertTo[Long]),
        fields.get("actualChocksTime").map(_.convertTo[Long]),
        fields.get("estimatedPcpStartTime").map(_.convertTo[Long]),
        fields.get("estimatedPcpEndTime").map(_.convertTo[Long]),
        fields.get("estimatedPcpPaxCount").map(_.convertTo[Int]),
        fields.get("status").map(_.convertTo[String]).getOrElse(""),
      )
      case unexpected => throw new Exception(s"Failed to parse FlightJson. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val flightJsonFormat: RootJsonFormat[FlightJson] = jsonFormat12(FlightJson.apply)

  implicit object jsonResponseFormat extends RootJsonFormat[FlightJsonResponse] {

    override def write(obj: FlightJsonResponse): JsValue = JsObject(Map(
      "periodStart" -> obj.periodStart.toJson,
      "periodEnd" -> obj.periodEnd.toJson,
      "flights" -> obj.flights.toJson,
    ))

    override def read(json: JsValue): FlightJsonResponse = throw new Exception("Not implemented")
  }
}
