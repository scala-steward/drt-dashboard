package uk.gov.homeoffice.drt.services.api.v1_1.serialiser

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.routes.api.v1_1.FlightApiV1_1Routes.{FlightJsonResponseV1_1, FlightJsonV1_1}
import uk.gov.homeoffice.drt.time.SDate

class FlightApiV1_1JsonFormatsTest extends AnyWordSpec with Matchers with QueueApiV1_1JsonFormats {
  "FlightJsonFormat should serialise and deserialise correctly" in {
    val queue = FlightJsonV1_1("LHR", "T2", "BA123", "JFK", "John F. Kennedy International Airport",
      SDate("2024-10-20T10:00").millisSinceEpoch,
      Some(SDate("2024-10-20T10:05").millisSinceEpoch),
      None,
      None,
      Some(SDate("2024-10-20T10:45").millisSinceEpoch),
      Some(150),
      "On Time",
      Some(Seq.empty),
    )
    val json = queue.toJson
    val deserialised = json.convertTo[FlightJsonV1_1]

    deserialised shouldEqual queue
  }

  "jsonResponseFormat should serialise and deserialise correctly" in {
    val start = SDate("2024-10-20T10:00")
    val end = SDate("2024-10-20T12:00")
    val flightJson = FlightJsonV1_1(
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
      status = "On Time",
      queuePaxCounts = Some(Seq.empty),
    )
    val response = FlightJsonResponseV1_1(start, end, Seq(flightJson))
    val json = response.toJson
    val deserialised = json.convertTo[FlightJsonResponseV1_1]

    deserialised shouldEqual response
  }
}
