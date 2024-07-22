package uk.gov.homeoffice.drt.routes

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.testkit.TestProbe
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.DefaultJsonProtocol.immSeqFormat
import spray.json._
import uk.gov.homeoffice.drt.jsonformats.PassengersSummaryFormat.JsonFormat
import uk.gov.homeoffice.drt.models.PassengersSummary
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{PortCode, Queues}
import uk.gov.homeoffice.drt.services.PassengerSummaryStreams.{Daily, Granularity, Hourly, Total}
import uk.gov.homeoffice.drt.time.LocalDate

import scala.concurrent.ExecutionContextExecutor

class PassengerRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  val probeGranularity: TestProbe = TestProbe("granularity")
  val probeTerminal: TestProbe = TestProbe("terminal")
  val stnSummary: PassengersSummary = PassengersSummary(
    "Central",
    "stn",
    None,
    2,
    1,
    Map(Queues.EeaDesk -> 1),
    None,
    None
  )

  val mockSummary: (LocalDate, LocalDate, Granularity, Option[Terminal]) => PortCode => Source[(Map[Queue, Int], Int, Option[Any]), NotUsed] =
    (_, _, granularity, maybeTerminal) => _ => {
      probeGranularity.ref ! granularity
      probeTerminal.ref ! maybeTerminal
      Source.single((Map(Queues.EeaDesk -> 1), 2, None))
    }

  "PassengerRoutes" should {
    val header = RawHeader("X-Forwarded-Email", "someone@somewhere.com")
    val startDate = "2020-01-01"
    val endDate = "2020-01-02"
    val dailyGranularity = "daily"
    val hourGranularity = "hourly"

    "call the corresponding port uri for the port and dates, given no granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "?port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockSummary) ~> check {
        probeGranularity.expectMsg(Total)
        probeTerminal.expectMsg(None)
        responseAs[String].parseJson shouldEqual Seq(stnSummary).toJson
      }
    }

    "call the corresponding port uri for the port and dates, given daily granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "?granularity=" + dailyGranularity + "&port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockSummary) ~> check {
        probeGranularity.expectMsg(Daily)
        probeTerminal.expectMsg(None)
        responseAs[String].parseJson shouldEqual Seq(stnSummary).toJson
      }
    }

    "call the corresponding port uri for the port and dates, given hourly granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "?granularity=" + hourGranularity + "&port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockSummary) ~> check {
        probeGranularity.expectMsg(Hourly)
        probeTerminal.expectMsg(None)
        responseAs[String].parseJson shouldEqual Seq(stnSummary).toJson
      }
    }

    val terminal = "t1"

    val stnSummaryWithTerminal = stnSummary.copy(terminalName = Option(terminal))

    "call the corresponding terminal uri for the port and dates, given no granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "/" + terminal + "?port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockSummary) ~> check {
        probeGranularity.expectMsg(Total)
        probeTerminal.expectMsg(Option(Terminal(terminal)))
        responseAs[String].parseJson shouldEqual Seq(stnSummaryWithTerminal).toJson
      }
    }

    "call the corresponding terminal uri for the port and dates, given daily granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + dailyGranularity + "&port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockSummary) ~> check {
        probeGranularity.expectMsg(Daily)
        probeTerminal.expectMsg(Option(Terminal(terminal)))
        responseAs[String].parseJson shouldEqual Seq(stnSummaryWithTerminal).toJson
      }
    }

    "call the corresponding terminal uri for the port and dates, given hourly granularity" in {
      Get("/passengers/" + startDate + "/" + endDate + "/" + terminal + "?granularity=" + hourGranularity + "&port-codes=stn") ~> addHeader(header) ~> PassengerRoutes(mockSummary) ~> check {
        probeGranularity.expectMsg(Hourly)
        probeTerminal.expectMsg(Option(Terminal(terminal)))
        responseAs[String].parseJson shouldEqual Seq(stnSummaryWithTerminal).toJson
      }
    }

    "call combine the output from each requested port" in {
      Get("/passengers/" + startDate + "/" + endDate + "/" + terminal + "?port-codes=stn,lhr") ~> addHeader(header) ~> PassengerRoutes(mockSummary) ~> check {
        val lhrSummaryWithTerminal = stnSummaryWithTerminal.copy(portCode = "lhr", regionName = "Heathrow")
        responseAs[String].parseJson shouldEqual Seq(stnSummaryWithTerminal, lhrSummaryWithTerminal).toJson
      }
    }
  }
}
