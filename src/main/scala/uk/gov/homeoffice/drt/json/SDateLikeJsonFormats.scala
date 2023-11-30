package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, JsNumber, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

object SDateLikeJsonFormats extends DefaultJsonProtocol {

  implicit object SDateLikeJsonFormat extends RootJsonFormat[SDateLike] {
    override def read(json: JsValue): SDateLike = json match {
      case JsNumber(ts) => SDate(ts.toLong)
    }

    override def write(obj: SDateLike): JsValue = obj.millisSinceEpoch.toJson
  }
}
