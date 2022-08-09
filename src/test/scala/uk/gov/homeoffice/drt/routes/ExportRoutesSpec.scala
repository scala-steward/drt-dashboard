package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.common.{ CsvEntityStreamingSupport, EntityStreamingSupport }
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.HttpClient

import scala.concurrent.{ ExecutionContextExecutor, Future }

class ExportRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  implicit val streamingSupport: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withContentType(ContentTypes.`text/csv(UTF-8)`)

  val csv =
    """IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

  val httpResponse: String => HttpResponse = csvString => HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, csvString))

  val mockHttpClient = new MockHttpClient(httpResponse(csv))

  "Request heathrow arrival export" should {
    "collate all terminal arrivals" in {
      Get("/export/Heathrow/2022-08-02/2022-08-03") ~> ExportRoutes(mockHttpClient) ~> check {
        val a = responseAs[String]
        a should ===(heathrowRegionPortTerminalData)
      }
    }
  }

  "Request North arrival export" should {
    "collate all port and terminal arrivals in the North region" in {
      Get("/export/North/2022-08-02/2022-08-03") ~> ExportRoutes(mockHttpClient) ~> check {
        val a = responseAs[String]
        a should ===(northRegionPortTerminalData)
      }
    }
  }

  "Request South arrival export" should {
    "collate all port and terminal arrivals in the North region" in {
      Get("/export/South/2022-08-02/2022-08-03") ~> ExportRoutes(mockHttpClient) ~> check {
        val a = responseAs[String]
        a should ===(southRegionPortTerminalData)
      }
    }
  }

  "Request Central arrival export" should {
    "collate all port and terminal arrivals in the North region" in {
      Get("/export/Central/2022-08-02/2022-08-03") ~> ExportRoutes(mockHttpClient) ~> check {
        val a = responseAs[String]
        a should ===(centralRegionPortTerminalData)
      }
    }
  }

  val heathrowRegionPortTerminalData =
    """Region,Port,Terminal,IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |Heathrow,LHR,T2,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T2,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T3,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T3,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T4,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T4,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T5,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T5,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

  val northRegionPortTerminalData =
    """Region,Port,Terminal,IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |North,LPL,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,LPL,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,ABZ,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,ABZ,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,MAN,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,MAN,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,MAN,T2,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,MAN,T2,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,MAN,T3,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,MAN,T3,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,DSA,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,DSA,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,NCL,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,NCL,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,EDI,A1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,EDI,A1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,EDI,A2,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,EDI,A2,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,HUY,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,HUY,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,LBA,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,LBA,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,BFS,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,BFS,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,MME,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,MME,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,PIK,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,PIK,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,GLA,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,GLA,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,INV,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,INV,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,BHD,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |North,BHD,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

  val southRegionPortTerminalData =
    """Region,Port,Terminal,IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |South,BOH,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,BOH,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,CWL,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,CWL,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,LGW,N,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,LGW,N,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,LGW,S,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,LGW,S,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,NQY,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,NQY,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,SEN,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,SEN,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,SOU,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,SOU,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,EXT,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,EXT,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,BRS,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |South,BRS,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

  val centralRegionPortTerminalData =
    """Region,Port,Terminal,IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |Central,STN,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,STN,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,NWI,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,NWI,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,LCY,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,LCY,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,EMA,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,EMA,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,BHX,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,BHX,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,BHX,T2,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,BHX,T2,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,LTN,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Central,LTN,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

}
