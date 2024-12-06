package uk.gov.homeoffice.drt.services.api.v1.serialiser

import spray.json._
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.{SlotJson, QueueJson, QueueJsonResponse}

trait QueueApiV1JsonFormats extends DefaultJsonProtocol with CommonJsonFormats {
  implicit object QueueJsonFormat extends RootJsonFormat[Queue] {
    override def write(obj: Queue): JsValue = obj.stringValue.toJson

    override def read(json: JsValue): Queue = json match {
      case JsString(value) => Queue(value)
      case unexpected => throw new Exception(s"Failed to parse Queue. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val queueJsonFormat: RootJsonFormat[QueueJson] = jsonFormat3(QueueJson.apply)

  implicit val periodJsonFormat: RootJsonFormat[SlotJson] = jsonFormat4(SlotJson.apply)

  implicit object jsonResponseFormat extends RootJsonFormat[QueueJsonResponse] {

    override def write(obj: QueueJsonResponse): JsValue = obj match {
      case obj: QueueJsonResponse => JsObject(Map(
        "periodStart" -> obj.periodStart.toJson,
        "periodEnd" -> obj.periodEnd.toJson,
        "periodLengthMinutes" -> obj.slotSizeMinutes.toJson,
        "periods" -> obj.slots.toJson,
      ))
    }

    override def read(json: JsValue): QueueJsonResponse = throw new Exception("Not implemented")
  }
}
