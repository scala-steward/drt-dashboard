package uk.gov.homeoffice.drt.rccu

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.Materializer
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.ports.PortRegion

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }

class ExportCsvServiceSpec extends Specification {
  val testKit: ActorTestKit = ActorTestKit()
  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContextExecutor = sys.executionContext
  val exportCsvService = new ExportCsvService(MockHttpClient)

  "Given a string of region" should {
    "get the portRegion mapped to name" in {
      val region = exportCsvService.getPortRegion("Heathrow")
      region must beSome(PortRegion.Heathrow)
    }
  }

  "Given port code LHR I get uri for csv export for the terminal" >> {
    val expectedUri = "http://lhr:9000/export/arrivals/2022-07-22/2022-07-24/T1"
    val uri = exportCsvService.getUri("LHR", "2022-07-22", "2022-07-24", "T1")
    uri mustEqual expectedUri
  }

  "Get response for given region port terminal" >> {
    val portResponse: Future[Option[PortResponse]] = exportCsvService.getPortResponseForTerminal("2022-07-22", "2022-07-23", "Heathrow", "Heathrow", "T2")
    val response = Await.result(portResponse, 1 seconds)
    response must beSome(PortResponse("Heathrow", "Heathrow", "T2", HttpResponse(entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, csv))))

  }

  object MockHttpClient extends HttpClient {
    def send(httpRequest: HttpRequest)(implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[HttpResponse] = {
      if (httpRequest.getUri().path().contains("PIK")) {
        Future.failed(new Exception("Server not found"))
      } else {
        Future.successful(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, csv)))
      }
    }
  }

  val csv =
    """IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

}
