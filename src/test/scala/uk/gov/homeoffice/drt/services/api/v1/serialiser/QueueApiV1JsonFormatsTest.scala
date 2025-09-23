package uk.gov.homeoffice.drt.services.api.v1.serialiser

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk
import uk.gov.homeoffice.drt.ports.Terminals.T2
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.{QueueJson, QueueJsonResponse, SlotJson}
import uk.gov.homeoffice.drt.time.SDate

class QueueApiV1JsonFormatsTest extends AnyWordSpec with Matchers with QueueApiV1JsonFormats {
  "QueueJsonFormat should serialise and deserialise correctly" in {
    val queue = QueueJson(EeaDesk, 100, 10)
    val json = queue.toJson
    val deserialised = json.convertTo[QueueJson]

    deserialised shouldEqual queue
  }

  "PeriodJsonFormat should serialise and deserialise correctly" in {
    val start = SDate("2024-10-20T10:00")
    val period = SlotJson(start, PortCode("LHR"), T2, Seq(QueueJson(EeaDesk, 100, 10)))
    val json = period.toJson
    val deserialised = json.convertTo[SlotJson]

    deserialised shouldEqual period
  }

  "jsonResponseFormat should serialise and deserialise correctly" in {
    val start = SDate("2024-10-20T10:00")
    val end = SDate("2024-10-20T12:00")
    val slotSizeMinutes = 15
    val slots = Seq(
      SlotJson(start, PortCode("LHR"), T2, Seq(QueueJson(EeaDesk, 100, 10))),
      SlotJson(start.addMinutes(15), PortCode("LHR"), T2, Seq(QueueJson(EeaDesk, 120, 12)))
    )
    val response = QueueJsonResponse(start, end, slotSizeMinutes, slots)
    val json = response.toJson
    val deserialised = json.convertTo[QueueJsonResponse]

    deserialised shouldEqual response
  }
}
