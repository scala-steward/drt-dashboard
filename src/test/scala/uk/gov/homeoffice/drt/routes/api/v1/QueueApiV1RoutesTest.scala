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
import uk.gov.homeoffice.drt.ports.Terminals.{T2, T3, Terminal}
import uk.gov.homeoffice.drt.ports.{PortCode, Queues}
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.{SlotJson, QueueJson, QueueJsonResponse}
import uk.gov.homeoffice.drt.services.api.v1.serialiser.QueueApiV1JsonFormats
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.{ExecutionContextExecutor, Future}

class QueueApiV1RoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest with QueueApiV1JsonFormats {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  val start: SDateLike = SDate("2024-10-20T10:00")
  val end: SDateLike = SDate("2024-10-20T12:00")

  val queueJson: QueueJson = QueueJson(Queues.EeaDesk, 100, 10)
  val periodJson: (PortCode, Terminal) => SlotJson = (pc, t) => SlotJson(start, pc, t, Seq(queueJson))
  val defaultSlotSizeMinutes = 15

  "Given a request for the queue status, I should see a JSON response containing the queue status" in {
    val routes = QueueApiV1Routes(
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
      dateRangeJsonForPortsAndSlotSize = (_, _) =>
        (_, _) => Future.successful(QueueJsonResponse(start, end, defaultSlotSizeMinutes, Seq(periodJson(PortCode("LHR"), T2), periodJson(PortCode("LHR"), T3)))),
    )
    Get("/queues?start=" + start.toISOString + "&end=" + end.toISOString) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-queue-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      val expected = QueueApiV1Routes.QueueJsonResponse(start, end, defaultSlotSizeMinutes, Seq(periodJson(PortCode("LHR"), T2), periodJson(PortCode("LHR"), T3)))

      responseAs[String] shouldEqual expected.toJson.compactPrint
    }
  }

  "Given a request without the optional slot-size-minutes parameter, the default slot size should be 15 minutes" in {
    val probe = TestProbe("queueApiV1Routes")
    val routes = QueueApiV1Routes(
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
      dateRangeJsonForPortsAndSlotSize = (_, slotSize) => (_, _) => {
        probe.ref ! slotSize
        Future.successful(QueueJsonResponse(start, end, defaultSlotSizeMinutes, Seq(periodJson(PortCode("LHR"), T2), periodJson(PortCode("LHR"), T3))))
      },
    )

    Get("/queues?start=" + start.toISOString + "&end=" + end.toISOString) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-queue-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {
      probe.expectMsg(defaultSlotSizeMinutes)
    }
  }

  "Given a failed response from a port the response status should be 500" in {
    val routes = QueueApiV1Routes(
      enabledPorts = Seq(PortCode("LHR"), PortCode("LGW")),
      dateRangeJsonForPortsAndSlotSize = (_, _) => (_, _) => Future.failed(new Exception("Failed to get flights")),
    )

    Get("/queues?start=" + start.toISOString + "&end=" + end.toISOString) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,api-queue-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      response.status.intValue() shouldEqual 500
    }
  }

  "Given a request from a user with access to some ports that are not enabled, only the enabled ports should be passed to the source function" in {
    val probe = TestProbe("queueApiV1Routes")
    val routes = QueueApiV1Routes(
      enabledPorts = Seq(PortCode("LHR")),
      dateRangeJsonForPortsAndSlotSize = (portCodes, _) => (_, _) => {
        probe.ref ! portCodes
        Future.successful(QueueJsonResponse(start, end, defaultSlotSizeMinutes, Seq.empty))
      },
    )

    Get("/queues?start=" + start.toISOString + "&end=" + end.toISOString) ~>
      RawHeader("X-Forwarded-Groups", "LHR,LGW,STN,api-queue-access") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {
      probe.expectMsg(Seq(PortCode("LHR")))
    }
  }

  "Given a request from a user without access to the queue api, the response should be 403" in {
    val routes = QueueApiV1Routes(
      enabledPorts = Seq(PortCode("LHR")),
      dateRangeJsonForPortsAndSlotSize =
        (_, _) => (_, _) => Future.successful(QueueJsonResponse(start, end, defaultSlotSizeMinutes, Seq(periodJson(PortCode("LHR"), T2), periodJson(PortCode("LHR"), T3)))),
    )

    Get("/queues?start=" + start.toISOString + "&end=" + end.toISOString) ~>
      RawHeader("X-Forwarded-Groups", "LHR") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~>
      routes ~> check {

      rejection.isInstanceOf[AuthorizationFailedRejection] shouldBe true
    }
  }
}
