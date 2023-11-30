package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError}
import uk.gov.homeoffice.drt.db.FeatureGuideRow
import uk.gov.homeoffice.drt.routes.FeaturePublished

import java.sql.Timestamp

trait DefaultTimeJsonProtocol extends DefaultJsonProtocol {
  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    override def write(obj: Timestamp): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Timestamp = json match {
      case JsString(rawDate) => {
        try {
          Timestamp.valueOf(rawDate)
        } catch {
          case iae: IllegalArgumentException => deserializationError("Invalid date format")
          case _: Exception => None
        }
      } match {
        case dateTime: Timestamp => dateTime
        case None => deserializationError(s"Couldn't parse date time, got $rawDate")
      }
    }
  }

  implicit val featureGuideRowFormatParser: RootJsonFormat[FeatureGuideRow] = jsonFormat6(FeatureGuideRow)

  implicit val featurePublishedFormatParser: RootJsonFormat[FeaturePublished] = jsonFormat1(FeaturePublished)

}
