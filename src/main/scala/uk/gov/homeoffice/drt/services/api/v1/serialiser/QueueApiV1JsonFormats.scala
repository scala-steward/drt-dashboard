package uk.gov.homeoffice.drt.services.api.v1.serialiser

import spray.json._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.QueueJsonResponse
import uk.gov.homeoffice.drt.services.api.v1.QueueExport.{PeriodJson, PortQueuesJson, QueueJson, TerminalQueuesJson}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

trait QueueApiV1JsonFormats extends DefaultJsonProtocol {

  implicit object QueueJsonFormat extends RootJsonFormat[Queue] {
    override def write(obj: Queue): JsValue = obj.stringValue.toJson

    override def read(json: JsValue): Queue = json match {
      case JsString(value) => Queue(value)
      case unexpected => throw new Exception(s"Failed to parse Queue. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val queueJsonFormat: RootJsonFormat[QueueJson] = jsonFormat3(QueueJson.apply)

  implicit object SDateJsonFormat extends RootJsonFormat[SDateLike] {
    override def write(obj: SDateLike): JsValue = obj.toISOString.toJson

    override def read(json: JsValue): SDateLike = json match {
      case JsString(value) => SDate(value)
      case unexpected => throw new Exception(s"Failed to parse SDate. Expected JsNumber. Got ${unexpected.getClass}")
    }
  }

  implicit val periodJsonFormat: RootJsonFormat[PeriodJson] = jsonFormat2(PeriodJson.apply)

  implicit object TerminalJsonFormat extends RootJsonFormat[Terminal] {
    override def write(obj: Terminal): JsValue = obj.toString.toJson

    override def read(json: JsValue): Terminal = json match {
      case JsString(value) => Terminal(value)
      case unexpected => throw new Exception(s"Failed to parse Terminal. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val terminalQueuesJsonFormat: RootJsonFormat[TerminalQueuesJson] = jsonFormat2(TerminalQueuesJson.apply)

  implicit object PortCodeJsonFormat extends RootJsonFormat[PortCode] {
    override def write(obj: PortCode): JsValue = obj.iata.toJson

    override def read(json: JsValue): PortCode = json match {
      case JsString(value) => PortCode(value)
      case unexpected => throw new Exception(s"Failed to parse Terminal. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val portQueuesJsonFormat: RootJsonFormat[PortQueuesJson] = jsonFormat2(PortQueuesJson.apply)

  implicit object jsonResponseFormat extends RootJsonFormat[QueueJsonResponse] {

    override def write(obj: QueueJsonResponse): JsValue = obj match {
      case obj: QueueJsonResponse => JsObject(Map(
        "startTime" -> obj.startTime.toJson,
        "endTime" -> obj.endTime.toJson,
        "periodLengthMinutes" -> obj.slotSizeMinutes.toJson,
        "ports" -> obj.ports.toJson,
      ))
    }

    override def read(json: JsValue): QueueJsonResponse = throw new Exception("Not implemented")
  }
}
