package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, JsString, JsValue, RootJsonFormat}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

object LocalDateJsonFormats extends DefaultJsonProtocol {
  implicit object LocalDateJsonFormat extends RootJsonFormat[LocalDate] {
    override def read(json: JsValue): LocalDate = json match {
      case JsString(s) => SDate(s).toLocalDate
    }

    override def write(obj: LocalDate): JsValue = JsString(obj.toString)
  }
}
