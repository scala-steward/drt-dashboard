package uk.gov.homeoffice.drt.services.api.v1_1.serialiser

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk
import uk.gov.homeoffice.drt.ports.Terminals.T2
import uk.gov.homeoffice.drt.routes.api.v1_1.QueueApiV1_1Routes.{QueueJsonResponseV1_1, QueueJsonV1_1, SlotJsonV1_1}
import uk.gov.homeoffice.drt.time.SDate

class QueueApiV1_1JsonFormatsTest extends AnyWordSpec with Matchers with QueueApiV1_1JsonFormats {
  "QueueJsonFormat should serialise and deserialise correctly" in {
    val queue = QueueJsonV1_1(EeaDesk, 100, 10)
    val json = queue.toJson
    val deserialised = json.convertTo[QueueJsonV1_1]

    deserialised shouldEqual queue
  }

  "PeriodJsonFormat should serialise and deserialise correctly" in {
    val start = SDate("2024-10-20T10:00")
    val period = SlotJsonV1_1(start, PortCode("LHR"), T2, Seq(QueueJsonV1_1(EeaDesk, 100, 10)))
    val json = period.toJson
    val deserialised = json.convertTo[SlotJsonV1_1]

    deserialised shouldEqual period
  }

  "jsonResponseFormat should serialise and deserialise correctly" in {
    val start = SDate("2024-10-20T10:00")
    val end = SDate("2024-10-20T12:00")
    val slotSizeMinutes = 15
    val slots = Seq(
      SlotJsonV1_1(start, PortCode("LHR"), T2, Seq(QueueJsonV1_1(EeaDesk, 100, 10))),
      SlotJsonV1_1(start.addMinutes(15), PortCode("LHR"), T2, Seq(QueueJsonV1_1(EeaDesk, 120, 12)))
    )
    val response = QueueJsonResponseV1_1(start, end, slotSizeMinutes, slots)
    val json = response.toJson
    val deserialised = json.convertTo[QueueJsonResponseV1_1]

    deserialised shouldEqual response
  }
}
