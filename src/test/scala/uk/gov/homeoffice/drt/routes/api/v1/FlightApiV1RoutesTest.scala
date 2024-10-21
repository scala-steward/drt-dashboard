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
import uk.gov.homeoffice.drt.routes.api.v1.RouteTestHelper.requestPortAndUriExist
import uk.gov.homeoffice.drt.{MockHttpClient, ProdHttpClient}

import scala.concurrent.{ExecutionContextExecutor, Future}

class FlightApiV1RoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  "Given a request for the flight status, I should see a JSON response containing the flight status" in {
    val portContent = """["some content"]"""
    val routes = FlightApiV1Routes(
      httpClient = MockHttpClient(() => portContent),
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )
    val start = "2024-10-20T10:00"
    val end = "2024-10-20T12:00"
    Get("/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-flight-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      responseAs[String] shouldEqual FlightApiV1Routes.JsonResponse(start, end, Seq(portContent, portContent)).toJson.compactPrint
    }
  }

  "Given a failed response from a port the response status should be 500" in {
    val routes = FlightApiV1Routes(
      httpClient = ProdHttpClient(_ => Future.failed(new RuntimeException("Failed to connect"))),
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )
    val start = "2024-10-20T10:00"
    val end = "2024-10-20T12:00"
    Get("/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-flight-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      response.status.intValue() shouldEqual 500
    }
  }

  "Given a request from a user with access to some ports that are not enabled, the response should only contain the enabled ports" in {
    val probe = TestProbe("flightApiV1Routes")
    val portContent = """["some content"]"""
    val routes = FlightApiV1Routes(httpClient = MockHttpClient(() => portContent, maybeProbe = Option(probe)), enabledPorts = Seq(PortCode("LHR")))
    val start = "2024-10-20T10:00"
    val end = "2024-10-20T12:00"
    Get("/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,STN,api-flight-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      requestPortAndUriExist(probe, "lhr", s"start=2024-10-20T10:00:00Z&end=2024-10-20T12:00:00Z")
    }
  }

  "Given a request from a user without access to the flight api, the response should be 403" in {
    val portContent = """["some content"]"""
    val routes = FlightApiV1Routes(httpClient = MockHttpClient(() => portContent), enabledPorts = Seq(PortCode("LHR")))
    val start = "2024-10-20T10:00"
    val end = "2024-10-20T12:00"
    Get("/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      rejection.isInstanceOf[AuthorizationFailedRejection] shouldBe true
    }
  }
}
