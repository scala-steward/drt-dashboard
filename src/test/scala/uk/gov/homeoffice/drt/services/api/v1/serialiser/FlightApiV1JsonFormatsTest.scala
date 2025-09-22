package uk.gov.homeoffice.drt.services.api.v1.serialiser

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.EeaDesk
import uk.gov.homeoffice.drt.ports.Terminals.T2
import uk.gov.homeoffice.drt.routes.api.v1.FlightApiV1Routes.{FlightJson, FlightJsonResponse}
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.{QueueJson, SlotJson}
import uk.gov.homeoffice.drt.time.SDate

class FlightApiV1JsonFormatsTest extends AnyWordSpec with Matchers with QueueApiV1JsonFormats {
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
    val flightJson = FlightJson(
      arrivalPortCode = "LHR",
      arrivalTerminal = "T2",
      code = "BA123",
      originPortIata = "JFK",
      originPortName = "John F. Kennedy International Airport",
      scheduledTime = start.millisSinceEpoch,
      estimatedLandingTime = Some(start.addMinutes(5).millisSinceEpoch),
      actualChocksTime = None,
      estimatedPcpStartTime = None,
      estimatedPcpEndTime = Some(start.addMinutes(45).millisSinceEpoch),
      estimatedPaxCount = Some(150),
      status = "On Time"
    )
    val response = FlightJsonResponse(start, end, Seq(flightJson))
    val json = response.toJson
    val deserialised = json.convertTo[FlightJsonResponse]

    deserialised shouldEqual response
  }
}
