package uk.gov.homeoffice.drt.routes.api.v1

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.{HttpClient, MockHttpClient, ProdHttpClient}

import scala.concurrent.{ExecutionContextExecutor, Future}

class FlightApiRoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  "Given a request for the queue status, I should see a JSON response containing the queue status" in {
    val portContent = """["some content"]"""
    val queueApiRoutes = FlightApiRoutes(
      httpClient = MockHttpClient(() => portContent),
      destinationPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )
    val start = "2024-10-20T10:00"
    val end = "2024-10-20T12:00"
    Get("/v1/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      queueApiRoutes ~> check {

      responseAs[String] shouldEqual FlightApiRoutes.JsonResponse(start, end, Seq(portContent, portContent)).toJson.compactPrint
    }
  }

  "Given a failed response from a port the response status should be 500" in {
    val queueApiRoutes = FlightApiRoutes(
      httpClient = ProdHttpClient(_ => Future.failed(new RuntimeException("Failed to connect"))),
      destinationPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )
    val start = "2024-10-20T10:00"
    val end = "2024-10-20T12:00"
    Get("/v1/flights?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      queueApiRoutes ~> check {

      response.status.intValue() shouldEqual 500
    }
  }
}
