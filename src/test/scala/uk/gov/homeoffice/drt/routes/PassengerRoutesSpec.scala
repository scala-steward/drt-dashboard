package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.testkit.TestProbe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol.immSeqFormat
import spray.json.enrichAny
import uk.gov.homeoffice.drt.MockHttpClient
import uk.gov.homeoffice.drt.jsonformats.PassengersSummaryFormat.JsonFormat
import uk.gov.homeoffice.drt.models.PassengersSummary
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.time.LocalDate

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

class PassengerRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  val probe: TestProbe = TestProbe("http-client")
  val passengersSummary: PassengersSummary = PassengersSummary(
    "regionName",
    "portCode",
    Some("terminalName"),
    1,
    Map(Queues.EeaDesk -> 1),
    Some(LocalDate(2020, 1, 1)),
    Some(1)
  )

  def mockHttp(summary: PassengersSummary): MockHttpClient = MockHttpClient(() => "[" + summary.toJson.compactPrint + "]", Option(probe))

  "PassengerRoutes" should {
    val portCode = "stn"
    val header = RawHeader("X-Forwarded-Email", "someone@somewhere.com")
    val startDate = "2020-01-01"
    val endDate = "2020-01-02"
    val defaultGranularity = "total"
    val dailyGranularity = "daily"
    val hourGranularity = "hourly"

    "call the corresponding port uri for the port and dates, given no granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "?port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockHttp(passengersSummary)) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "?granularity=" + defaultGranularity
            val uriOk = request.uri.toString() == expectedUri
            val headersOk = request.headers.contains(header)

            uriOk && headersOk
        }
        val str = responseAs[String]

        str shouldEqual Seq(passengersSummary).toJson.compactPrint
      }
    }

    "call the corresponding port uri for the port and dates, given daily granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "?granularity=" + dailyGranularity + "&port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockHttp(passengersSummary)) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "?granularity=" + dailyGranularity
            request.uri.toString() == expectedUri
        }
        responseAs[String] shouldEqual Seq(passengersSummary).toJson.compactPrint
      }
    }

    "call the corresponding port uri for the port and dates, given hourly granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "?granularity=" + hourGranularity + "&port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockHttp(passengersSummary)) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "?granularity=" + hourGranularity
            request.uri.toString() == expectedUri
        }
        responseAs[String] shouldEqual Seq(passengersSummary).toJson.compactPrint
      }
    }

    val terminal = "t1"

    "call the corresponding terminal uri for the port and dates, given no granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "/" + terminal + "?port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockHttp(passengersSummary)) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + defaultGranularity
            val uriOk = request.uri.toString() == expectedUri
            val headersOk = request.headers.contains(header)
            uriOk && headersOk
        }
        responseAs[String] shouldEqual Seq(passengersSummary).toJson.compactPrint
      }
    }

    "call the corresponding terminal uri for the port and dates, given daily granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + dailyGranularity + "&port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockHttp(passengersSummary)) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + dailyGranularity
            println(s"request.uri.toString() == expectedUri: ${request.uri.toString()}")
            request.uri.toString() == expectedUri
        }
        responseAs[String] shouldEqual Seq(passengersSummary).toJson.compactPrint
      }
    }

    "call the corresponding terminal uri for the port and dates, given hourly granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + hourGranularity + "&port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockHttp(passengersSummary)) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + hourGranularity
            request.uri.toString() == expectedUri
        }
        responseAs[String] shouldEqual Seq(passengersSummary).toJson.compactPrint
      }
    }

    "call combine the output from each requested port" in {
      Get("/passengers/" + startDate + "/" + endDate + "/" + terminal + "?port-codes=stn,lhr") ~> addHeader(header) ~> PassengerRoutes(mockHttp(passengersSummary)) ~> check {
        responseAs[String] shouldEqual Seq(passengersSummary, passengersSummary).toJson.compactPrint
      }
    }
  }
}
