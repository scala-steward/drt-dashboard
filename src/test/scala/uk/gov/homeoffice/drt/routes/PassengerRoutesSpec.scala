package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.testkit.TestProbe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.MockHttpClient

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

class PassengerRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  val probe = TestProbe("http-client")
  val mockContent = "stuff"
  val mockHttp = MockHttpClient(() => mockContent, Option(probe))

  "PassengerRoutes" should {
    val header = RawHeader("X-Auth-Email", "someone@somewhere.com")
    val portCode = "stn"
    val startDate = "2020-01-01"
    val endDate = "2020-01-02"
    val defaultGranularity = "total"
    val dailyGranularity = "daily"
    val hourGranularity = "hourly"

    "call the corresponding port uri for the port and dates, given no granularity" in {
      Get("/passengers/" + portCode + "/" + startDate + "/" + endDate) ~> addHeader(header) ~> PassengerRoutes(mockHttp) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "?granularity=" + defaultGranularity
            val uriOk = request.uri.toString() == expectedUri
            val headersOk = request.headers.contains(header)
            uriOk && headersOk
        }
        responseAs[String] shouldEqual mockContent
      }
    }

    "call the corresponding port uri for the port and dates, given daily granularity" in {
      Get("/passengers/" + portCode + "/" + startDate + "/" + endDate + "?granularity=" + dailyGranularity) ~> addHeader(header) ~> PassengerRoutes(mockHttp) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "?granularity=" + dailyGranularity
            request.uri.toString() == expectedUri
        }
        responseAs[String] shouldEqual mockContent
      }
    }

    "call the corresponding port uri for the port and dates, given hourly granularity" in {
      Get("/passengers/" + portCode + "/" + startDate + "/" + endDate + "?granularity=" + hourGranularity) ~> addHeader(header) ~> PassengerRoutes(mockHttp) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "?granularity=" + hourGranularity
            request.uri.toString() == expectedUri
        }
        responseAs[String] shouldEqual mockContent
      }
    }

    val terminal = "t1"

    "call the corresponding terminal uri for the port and dates, given no granularity" in {
      Get("/passengers/" + portCode + "/" + startDate + "/" + endDate + "/" + terminal) ~> addHeader(header) ~> PassengerRoutes(mockHttp) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + defaultGranularity
            val uriOk = request.uri.toString() == expectedUri
            val headersOk = request.headers.contains(header)
            uriOk && headersOk
        }
        responseAs[String] shouldEqual mockContent
      }
    }

    "call the corresponding terminal uri for the port and dates, given daily granularity" in {
      Get("/passengers/" + portCode + "/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + dailyGranularity) ~> addHeader(header) ~> PassengerRoutes(mockHttp) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + dailyGranularity
            println(s"request.uri.toString() == expectedUri: ${request.uri.toString()}")
            request.uri.toString() == expectedUri
        }
        responseAs[String] shouldEqual mockContent
      }
    }

    "call the corresponding terminal uri for the port and dates, given hourly granularity" in {
      Get("/passengers/" + portCode + "/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + hourGranularity) ~> addHeader(header) ~> PassengerRoutes(mockHttp) ~> check {
        probe.fishForMessage(1.second) {
          case request: HttpRequest =>
            val expectedUri = "http://" + portCode + ":9000/api/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + hourGranularity
            request.uri.toString() == expectedUri
        }
        responseAs[String] shouldEqual mockContent
      }
    }
  }
}
