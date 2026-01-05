package uk.gov.homeoffice.drt.services.api.v1_1.serialiser

import spray.json.{DefaultJsonProtocol, JsObject, JsString, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.routes.api.v1_1.FlightApiV1_1Routes.{FlightJsonResponseV1_1, FlightJsonV1_1, FlightQueuePaxCountJsonV1_1}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

trait FlightApiV1_1JsonFormats extends DefaultJsonProtocol with CommonJsonFormatsV1_1 {
  implicit val flightQueuePaxCountJsonFormat: RootJsonFormat[FlightQueuePaxCountJsonV1_1] = jsonFormat2(FlightQueuePaxCountJsonV1_1.apply)

  implicit object FlightJsonJsonFormat extends RootJsonFormat[FlightJsonV1_1] {
    override def write(obj: FlightJsonV1_1): JsValue = {
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
        "status" -> obj.status.toJson,
        "queuePaxCounts" -> obj.queuePaxCounts.toJson,
      )
    }

    override def read(json: JsValue): FlightJsonV1_1 = json match {
      case JsObject(fields) =>
        FlightJsonV1_1(
          fields.get("arrivalPortCode").map(_.convertTo[String]).getOrElse(""),
          fields.get("arrivalTerminal").map(_.convertTo[String]).getOrElse(""),
          fields.get("code").map(_.convertTo[String]).getOrElse(""),
          fields.get("originPortIata").map(_.convertTo[String]).getOrElse(""),
          fields.get("originPortName").map(_.convertTo[String]).getOrElse(""),
          fields.get("scheduledTime").map(st => SDate(st.convertTo[String]).millisSinceEpoch).getOrElse(0L),
          maybeSinceUnixEpochFromString(fields.get("estimatedLandingTime")),
          maybeSinceUnixEpochFromString(fields.get("actualChocksTime")),
          maybeSinceUnixEpochFromString(fields.get("estimatedPcpStartTime")),
          maybeSinceUnixEpochFromString(fields.get("estimatedPcpEndTime")),
          fields.get("estimatedPcpPaxCount").map(_.convertTo[Int]),
          fields.get("status").map(_.convertTo[String]).getOrElse(""),
          fields.get("queuePaxCounts").map(_.convertTo[Seq[FlightQueuePaxCountJsonV1_1]]),
        )
      case unexpected => throw new Exception(s"Failed to parse FlightJson. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  private def maybeSinceUnixEpochFromString(maybeValue: Option[JsValue]) = {
    maybeValue match {
      case Some(JsString(s)) if s.nonEmpty => Some(SDate(s).millisSinceEpoch)
      case _ => None
    }
  }

  implicit val flightJsonFormat: RootJsonFormat[FlightJsonV1_1] = jsonFormat13(FlightJsonV1_1.apply)

  implicit object jsonResponseFormat extends RootJsonFormat[FlightJsonResponseV1_1] {

    override def write(obj: FlightJsonResponseV1_1): JsValue = JsObject(Map(
      "periodStart" -> obj.periodStart.toJson,
      "periodEnd" -> obj.periodEnd.toJson,
      "flights" -> obj.flights.toJson,
    ))

    override def read(json: JsValue): FlightJsonResponseV1_1 = json match {
      case JsObject(fields) =>
        FlightJsonResponseV1_1(
          periodStart = fields("periodStart").convertTo[SDateLike],
          periodEnd = fields("periodEnd").convertTo[SDateLike],
          flights = fields("flights").convertTo[Seq[FlightJsonV1_1]],
        )
      case unexpected => throw new Exception(s"Failed to parse FlightJsonResponse. Expected JsObject. Got ${unexpected.getClass}")
    }
  }
}
