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

  implicit val streamingSupport: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withContentType(ContentTypes.`text/plain(UTF-8)`)

  "Request heathrow arrival export" should {
    "collate all terminal arrivals" in {
      Get("/export/Heathrow/2022-08-02/2022-08-03") ~> ExportRoutes(MockHttpClient) ~> check {
        val a = responseAs[String]
        a should ===(amendRegionPortTerminalcsv)
      }
    }
  }

  object MockHttpClient extends HttpClient {
    def send(httpRequest: HttpRequest)(implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[HttpResponse] = {
      Future(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, csv)))(executionContext)
    }
  }

  val amendRegionPortTerminalcsv =
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

  val csv =
    """IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

}
