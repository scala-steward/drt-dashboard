package uk.gov.homeoffice.drt.json

import org.joda.time.DateTime
import spray.json.{JsNumber, JsString, JsValue, RootJsonFormat}

object JodaDateTimeJsonFormat {
  implicit object jodaDateTimeJsonFormat extends RootJsonFormat[DateTime] {
    override def read(json: JsValue): DateTime = json match {
      case JsNumber(millis) => new DateTime(millis.toLong)
      case JsString(str) => DateTime.parse(str)
      case _ => throw new Exception("Failed to deserialise DateTime json")
    }

    override def write(obj: DateTime): JsValue = JsNumber(obj.getMillis)
  }
}
