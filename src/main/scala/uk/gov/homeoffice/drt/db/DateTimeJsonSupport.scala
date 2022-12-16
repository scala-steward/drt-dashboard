package uk.gov.homeoffice.drt.db

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.DateTime
import spray.json.{ DefaultJsonProtocol, JsString, JsValue, JsonFormat, deserializationError }

import java.sql.Timestamp

trait DateTimeJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object DateTimeFormat extends JsonFormat[Timestamp] {
    override def write(obj: Timestamp): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Timestamp = json match {
      case JsString(rawDate) => {
        try {
          DateTime.parse(rawDate).asInstanceOf[Timestamp]
        } catch {
          case _: IllegalArgumentException => deserializationError("Invalid date format")
          case _: Exception => deserializationError(s"Couldn't parse date time, got $rawDate")
        }
      }
    }
  }
}
