package uk.gov.homeoffice.drt.routes.api.v1

import akka.actor.typed.ActorSystem
import akka.http.javadsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.testkit.TestProbe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.T2
import uk.gov.homeoffice.drt.routes.api.v1.FlightApiV1Routes.FlightJsonResponse
import uk.gov.homeoffice.drt.services.api.v1.FlightExport.{FlightJson, PortFlightsJson, TerminalFlightsJson}
import uk.gov.homeoffice.drt.services.api.v1.serialiser.FlightApiV1JsonFormats
import uk.gov.homeoffice.drt.time.SDate

import scala.concurrent.{ExecutionContextExecutor, Future}

class FlightApiV1RoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest with FlightApiV1JsonFormats {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  val start = "2024-10-20T10:00"
  val end = "2024-10-20T12:00"

  val flightJson: FlightJson = FlightJson(
    "BA0001",
    "LHR",
    "Heathrow",
    1600000000000L,
    Some(1600000000000L),
    Some(1600000000000L),
    Some(1600000000000L),
    Some(1600000000000L),
    Option(100),
    "scheduled"
  )
  val terminalFlightJson: TerminalFlightsJson = TerminalFlightsJson(T2, Seq(flightJson))
  val portFlightJsonLhr: PortFlightsJson = PortFlightsJson(PortCode("LHR"), Seq(terminalFlightJson))
  val portFlightJsonStn: PortFlightsJson = PortFlightsJson(PortCode("STN"), Seq(terminalFlightJson))

  "Given a request for the flight status, I should see a JSON response containing the flight status" in {
    val routes = FlightApiV1Routes(
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
      dateRangeJsonForPorts = _ => (_, _) => Future.successful(FlightJsonResponse(SDate(start), SDate(end), Seq(portFlightJsonLhr, portFlightJsonLhr))),
    )

    Get("/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-flight-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      val expected = FlightApiV1Routes.FlightJsonResponse(SDate(start), SDate(end), Seq(portFlightJsonLhr, portFlightJsonLhr))
      responseAs[String] shouldEqual expected.toJson.compactPrint
    }
  }

  "Given a failed response from a port the response status should be 500" in {
    val routes = FlightApiV1Routes(
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
      dateRangeJsonForPorts = _ => (_, _) => Future.failed(new Exception("Failed to get flights")),
    )

    Get("/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-flight-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      response.status.intValue() shouldEqual 500
    }
  }

  "Given a request from a user with access to some ports that are not enabled, only the enabled ports should be passed to the source function" in {
    val probe = TestProbe("flight-source")
    val routes = FlightApiV1Routes(
      enabledPorts = Seq(PortCode("LHR")),
      dateRangeJsonForPorts = portCodes => (_, _) => {
        probe.ref ! portCodes
        Future.successful(FlightJsonResponse(SDate(start), SDate(end), Seq.empty))
      },
    )

    Get("/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,STN,api-flight-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {
      probe.expectMsg(Seq(PortCode("LHR")))
    }
  }

  "Given a request from a user without access to the flight api, the response should be 403" in {
    val routes = FlightApiV1Routes(
      enabledPorts = Seq(PortCode("LHR")),
      dateRangeJsonForPorts = _ => (_, _) => Future.successful(FlightJsonResponse(SDate(start), SDate(end), Seq.empty)),
    )

    Get("/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      rejection.isInstanceOf[AuthorizationFailedRejection] shouldBe true
    }
  }
}
