package uk.gov.homeoffice.drt.routes.api.v1

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.testkit.TestProbe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.{MockHttpClient, ProdHttpClient}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

class QueueApiV1RoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  "Given a request for the queue status, I should see a JSON response containing the queue status" in {
    val portContent = """["some content"]"""
    val queueApiRoutes = QueueApiV1Routes(
      httpClient = MockHttpClient(() => portContent),
      destinationPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )
    val start = "2024-10-20T10:00"
    val end = "2024-10-20T12:00"
    Get("/queues?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      queueApiRoutes ~> check {

      val defaultSlotSizeMinutes = 15
      responseAs[String] shouldEqual QueueApiV1Routes.JsonResponse(start, end, defaultSlotSizeMinutes, Seq(portContent, portContent)).toJson.compactPrint
    }
  }

  "Given a request without the optional slot-size-minutes parameter, the default slot size should be 15 minutes" in {
    val probe = TestProbe("queueApiV1Routes")
    val portContent = """["some content"]"""
    val queueApiRoutes = QueueApiV1Routes(
      httpClient = MockHttpClient(() => portContent, maybeProbe = Option(probe)),
      destinationPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )
    Get("/queues?start=" + "2024-10-20T10:00" + "&end=" + "2024-10-20T12:00") ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      queueApiRoutes ~> check {
      val defaultSlotSizeMinutes = "15"

      probe.fishForMessage(1.second) {
        case req: HttpRequest =>
          req.uri.toString.contains("lhr") &&
            req.uri.toString.contains("start=2024-10-20T10:00:00Z&end=2024-10-20T12:00:00Z&period-minutes=" + defaultSlotSizeMinutes)
      }
      probe.fishForMessage(1.second) {
        case req: HttpRequest =>
          req.uri.toString.contains("lgw") &&
            req.uri.toString.contains("start=2024-10-20T10:00:00Z&end=2024-10-20T12:00:00Z&period-minutes=" + defaultSlotSizeMinutes)
      }
    }
  }

  "Given a failed response from a port the response status should be 500" in {
    val queueApiRoutes = QueueApiV1Routes(
      httpClient = ProdHttpClient(_ => Future.failed(new RuntimeException("Failed to connect"))),
      destinationPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )
    val start = "2024-10-20T10:00"
    val end = "2024-10-20T12:00"
    Get("/queues?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      queueApiRoutes ~> check {

      response.status.intValue() shouldEqual 500
    }
  }

}
