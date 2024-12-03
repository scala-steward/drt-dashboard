package uk.gov.homeoffice.drt.services.api.v1.serialiser

import spray.json.DefaultJsonProtocol.StringJsonFormat
import spray.json.{JsString, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

trait CommonJsonFormats {
  implicit object SDateLikeISOJsonFormat extends RootJsonFormat[SDateLike] {
    override def read(json: JsValue): SDateLike = json match {
      case JsString(dateStr) => SDate(dateStr)
    }

    override def write(obj: SDateLike): JsValue = obj.toISOString.toJson
  }

  implicit object PortCodeJsonFormat extends RootJsonFormat[PortCode] {
    override def write(obj: PortCode): JsValue = obj.iata.toJson

    override def read(json: JsValue): PortCode = json match {
      case JsString(value) => PortCode(value)
      case unexpected => throw new Exception(s"Failed to parse Terminal. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit object TerminalJsonFormat extends RootJsonFormat[Terminal] {
    override def write(obj: Terminal): JsValue = obj.toString.toJson

    override def read(json: JsValue): Terminal = json match {
      case JsString(value) => Terminal(value)
      case unexpected => throw new Exception(s"Failed to parse Terminal. Expected JsString. Got ${unexpected.getClass}")
    }
  }
}
