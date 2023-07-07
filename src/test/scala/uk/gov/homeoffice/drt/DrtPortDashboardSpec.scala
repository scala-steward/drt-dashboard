package uk.gov.homeoffice.drt

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.AfterEach
import uk.gov.homeoffice.drt.services.drt.FeedJsonSupport._
import uk.gov.homeoffice.drt.services.drt._

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

class DrtPortDashboardSpec extends TestKit(ActorSystem("testActorSystem", ConfigFactory.empty()))
  with SpecificationLike
  with AfterEach {

  override def after: Unit = TestKit.shutdownActorSystem(system)

  "When querying DRT feed status endpoint to find App Status" >> {

    "I should be able to deserialize a response with 1 FeedStatus in it" >> {
      val mockClient = MockClient(singleStatusResponse)

      val result: immutable.Seq[FeedSourceStatus] = Await.result(
        mockClient.get("")
          .flatMap(res => Unmarshal[HttpResponse](res).to[List[FeedSourceStatus]]), 1.second)

      val expected = List(
        FeedSourceStatus(
          "LiveFeedSource",
          FeedStatuses(List("100001"), List("100002"), List("100003"))))

      result === expected
    }

    "I should be able to deserialize a response with 1 FeedStatus in it" >> {
      val mockClient = MockClient(multistatusResponse)

      val result: immutable.Seq[FeedSourceStatus] = Await.result(
        mockClient.get("")
          .flatMap(res => Unmarshal[HttpResponse](res).to[List[FeedSourceStatus]]), 1.second)

      val expected = List(
        FeedSourceStatus(
          "LiveFeedSource",
          FeedStatuses(List("100001"), List("100002"), List("100003"))),
        FeedSourceStatus(
          "LiveBaseFeedSource",
          FeedStatuses(List("200001"), List("200002"), List("200003"))))

      result === expected
    }
  }

  "When querying the DRT statuses endpoint for TST port code" >> {

    implicit val mat: ActorMaterializer = ActorMaterializer()

    "Given zero statuses in the response then I should get a DashboardPortStatus with no feeds in it" >> {
      val drtResponse = List[FeedSourceStatus]()

      val expected = DashboardPortStatus("TST", PortFeedStatuses(List()))

      val result = DashboardPortStatus("TST", drtResponse)

      result === expected
    }

    "Given one status in the response I should convert the response to a DashboardPortStatus containing it" >> {
      val drtResponse = List(
        FeedSourceStatus(
          "LiveFeedSource",
          FeedStatuses(List("100001"), List("100002"), List("100003"))))

      val expected = DashboardPortStatus("TST", PortFeedStatuses(List(
        PortFeedStatus(
          "LiveFeedSource",
          lastSuccessAt = Option(100001),
          lastFailureAt = Option(100002),
          lastUpdatesAt = Option(100003)))))

      val result = DashboardPortStatus("TST", drtResponse)

      result === expected

    }
    "Given two statuses in the response I should convert the response to a DashboardPortStatus containing both" >> {
      val drtResponse = List(
        FeedSourceStatus(
          "LiveFeedSource",
          FeedStatuses(List("100001"), List("100002"), List("100003"))),
        FeedSourceStatus(
          "LiveBaseFeedSource",
          FeedStatuses(List("200001"), List("200002"), List("200003"))))

      val expected = DashboardPortStatus("TST", PortFeedStatuses(List(
        PortFeedStatus(
          "LiveFeedSource",
          lastSuccessAt = Option(100001),
          lastFailureAt = Option(100002),
          lastUpdatesAt = Option(100003)),
        PortFeedStatus(
          "LiveBaseFeedSource",
          lastSuccessAt = Option(200001),
          lastFailureAt = Option(200002),
          lastUpdatesAt = Option(200003)))))

      val result = DashboardPortStatus("TST", drtResponse)

      result === expected

    }
  }

  case class MockClient(mockResponse: String) {
    def get(path: String): Future[HttpResponse] =
      Future(HttpResponse(200, Nil, HttpEntity(ContentTypes.`application/json`, mockResponse)))
  }

  def singleStatusResponse =
    """
      |    [{
      |            "feedSource": "LiveFeedSource",
      |            "feedStatuses": {
      |                "statuses": [
      |                    {
      |                        "$type": "drt.shared.FeedStatusSuccess",
      |                        "date": "100001",
      |                        "updateCount": 0
      |                    },
      |                    {
      |                        "$type": "drt.shared.FeedStatusSuccess",
      |                        "date": "1583149411413",
      |                        "updateCount": 0
      |                    },
      |                    {
      |                        "$type": "drt.shared.FeedStatusSuccess",
      |                        "date": "1583149351555",
      |                        "updateCount": 0
      |                    },
      |                    {
      |                        "$type": "drt.shared.FeedStatusSuccess",
      |                        "date": "1583149291495",
      |                        "updateCount": 0
      |                    }
      |                ],
      |                "lastSuccessAt": [
      |                    "100001"
      |                ],
      |                "lastFailureAt": [
      |                    "100002"
      |                ],
      |                "lastUpdatesAt": [
      |                    "100003"
      |                ]
      |            }
      |        }]
      |""".stripMargin

  def multistatusResponse =
    """
      |    [
      |        {
      |            "feedSource": "LiveFeedSource",
      |            "feedStatuses": {
      |                "statuses": [
      |
      |                ],
      |                "lastSuccessAt": [
      |                    "100001"
      |                ],
      |                "lastFailureAt": [
      |                    "100002"
      |                ],
      |                "lastUpdatesAt": [
      |                    "100003"
      |                ]
      |            }
      |        },
      |        {
      |            "feedSource": "LiveBaseFeedSource",
      |            "feedStatuses": {
      |                "statuses": [
      |
      |                ],
      |                "lastSuccessAt": [
      |                    "200001"
      |                ],
      |                "lastFailureAt": [
      |                    "200002"
      |                ],
      |                "lastUpdatesAt": [
      |                    "200003"
      |                ]
      |            }
      |    }]
      |""".stripMargin
}
