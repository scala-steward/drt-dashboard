package uk.gov.homeoffice.drt.services.api.v1.serialiser

import spray.json._
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.QueueJsonResponse
import uk.gov.homeoffice.drt.services.api.v1.QueueExport.{PeriodJson, PortQueuesJson, QueueJson, TerminalQueuesJson}

trait QueueApiV1JsonFormats extends DefaultJsonProtocol with CommonJsonFormats {
  implicit object QueueJsonFormat extends RootJsonFormat[Queue] {
    override def write(obj: Queue): JsValue = obj.stringValue.toJson

    override def read(json: JsValue): Queue = json match {
      case JsString(value) => Queue(value)
      case unexpected => throw new Exception(s"Failed to parse Queue. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val queueJsonFormat: RootJsonFormat[QueueJson] = jsonFormat3(QueueJson.apply)

  implicit val periodJsonFormat: RootJsonFormat[PeriodJson] = jsonFormat2(PeriodJson.apply)

  implicit val terminalQueuesJsonFormat: RootJsonFormat[TerminalQueuesJson] = jsonFormat2(TerminalQueuesJson.apply)

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
