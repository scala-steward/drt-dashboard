package uk.gov.homeoffice.drt.routes.api.v1

import akka.actor.typed.ActorSystem
import akka.http.javadsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.testkit.TestProbe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.api.v1.AuthApiV1Routes.JsonResponse
import uk.gov.homeoffice.drt.routes.api.v1.RouteTestHelper.requestPortAndUriExist
import uk.gov.homeoffice.drt.{MockHttpClient, ProdHttpClient}

import scala.concurrent.{ExecutionContextExecutor, Future}

class QueueApiV1RoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest with QueueApiV1JsonFormats {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  val start = "2024-10-20T10:00"
  val end = "2024-10-20T12:00"

  "Given a request for the queue status, I should see a JSON response containing the queue status" in {
    val portContent = """["some content"]"""
    val routes = QueueApiV1Routes(
      httpClient = MockHttpClient(() => portContent),
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )
    Get("/queues?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-queue-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      val defaultSlotSizeMinutes = 15
      val expected: JsonResponse = QueueApiV1Routes.QueueJsonResponse(start, end, defaultSlotSizeMinutes, Seq(portContent, portContent))

      responseAs[String] shouldEqual expected.toJson.compactPrint
    }
  }

  "Given a request without the optional slot-size-minutes parameter, the default slot size should be 15 minutes" in {
    val probe = TestProbe("queueApiV1Routes")
    val portContent = """["some content"]"""
    val routes = QueueApiV1Routes(
      httpClient = MockHttpClient(() => portContent, maybeProbe = Option(probe)),
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )

    Get("/queues?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-queue-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {
      val defaultSlotSizeMinutes = "15"

      requestPortAndUriExist(probe, "lhr", s"start=$start&end=$end&period-minutes=$defaultSlotSizeMinutes")
      requestPortAndUriExist(probe, "lgw", s"start=$start&end=$end&period-minutes=$defaultSlotSizeMinutes")
    }
  }

  "Given a failed response from a port the response status should be 500" in {
    val routes = QueueApiV1Routes(
      httpClient = ProdHttpClient(_ => Future.failed(new RuntimeException("Failed to connect"))),
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
    )

    Get("/queues?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-queue-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      response.status.intValue() shouldEqual 500
    }
  }

  "Given a request from a user with access to some ports that are not enabled, the response should only contain the enabled ports" in {
    val probe = TestProbe("queueApiV1Routes")
    val portContent = """["some content"]"""
    val routes = QueueApiV1Routes(httpClient = MockHttpClient(() => portContent, maybeProbe = Option(probe)), enabledPorts = Seq(PortCode("LHR")))

    Get("/queues?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,STN,api-queue-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      requestPortAndUriExist(probe, "lhr", s"start=$start&end=$end")
    }
  }

  "Given a request from a user without access to the queue api, the response should be 403" in {
    val portContent = """["some content"]"""
    val routes = QueueApiV1Routes(httpClient = MockHttpClient(() => portContent), enabledPorts = Seq(PortCode("LHR")))

    Get("/queues?start=" + start + "&end=" + end) ~>
      RawHeader("X-Forwarded-Groups", "LHR") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      rejection.isInstanceOf[AuthorizationFailedRejection] shouldBe true
    }
  }
}
