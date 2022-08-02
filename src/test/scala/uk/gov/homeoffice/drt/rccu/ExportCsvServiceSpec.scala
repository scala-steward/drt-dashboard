package uk.gov.homeoffice.drt.rccu

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.{ IOResult, Materializer }
import com.typesafe.config.{ Config, ConfigFactory }
import org.joda.time.{ DateTime, DateTimeZone }
import org.specs2.mutable.Specification
import org.specs2.specification.{ AfterEach, BeforeEach }
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.ports.PortRegion.Heathrow
import uk.gov.homeoffice.drt.routes.ExportRoutes

import java.io.File
import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }

class ExportCsvServiceSpec extends Specification with AfterEach with BeforeEach {
  private val config: Config = ConfigFactory.load()

  val testKit: ActorTestKit = ActorTestKit()

  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContextExecutor = sys.executionContext
  val exportCsvService = new ExportCsvService(MockHttpClient)

  val testFolder = config.getString("dashboard.file-store")

  override def before: Unit = {
    val file = new File(testFolder)
    if (file.exists()) file.listFiles.map(_.delete())
    else file.mkdirs()
  }

  override def after: Unit = {
    val file = new File(testFolder)
    if (file.exists()) file.listFiles.map(_.delete())
    file.deleteOnExit()
  }

  "Give date I get string in expected format" >> {
    val currentDateString = ExportRoutes.stringToDate("2022-07-22")
    val dateTime: DateTime = new DateTime(2022, 7, 22, 0, 0, 0, 0, DateTimeZone.forID("Europe/London"))
    currentDateString mustEqual dateTime
  }

  "Given port code LHR I get uri for csv export for the terminal" >> {
    val expectedUri = "http://lhr:9000/export/arrivals/2022-07-22/2022-07-24/T1"
    val uri = exportCsvService.getUri("LHR", "2022-07-22", "2022-07-24", "T1")
    uri mustEqual expectedUri
  }

  "Given port get the csv export for port and terminal" >> {
    val portResponses: Set[PortResponse] = Await.result(Future.sequence(exportCsvService.getPortResponseForRegionPorts("2022-07-22", "2022-07-22", Heathrow)), 1.seconds)
    val resultCsv: Set[Option[String]] = portResponses.map(r => r.httpResponse.map(r => Await.result(r.entity.dataBytes.runReduce(_ ++ _), 1.seconds).utf8String))
    resultCsv mustEqual Set(Option(csv))
  }

  "Given port get the csv export for port and terminal in List for Heathrow" >> {
    val fileName = ExportRoutes.makeFileName("test", "2022-07-22", "2022-07-23", "Heathrow")
    val portResponses: Future[Set[PortResponse]] = Future.sequence(exportCsvService.getPortResponseForRegionPorts("2022-07-22", "2022-07-23", Heathrow))
      .map(_.filter(_.httpResponse.isDefined))
    val ioResultF: Future[Set[IOResult]] = portResponses
      .map(r => Future.sequence(r.flatMap(exportCsvService.getCsvDataRegionPort(_, s"$testFolder/$fileName", false))))
      .flatten
    val resultCsvs: Set[IOResult] = Await.result(ioResultF, 1.seconds)
    resultCsvs.size mustEqual 1
  }

  "Given a string I get PortRegion" >> {
    val region = ExportRoutes.getPortRegion("Heathrow")
    PortRegion.Heathrow mustEqual region
  }

  object MockHttpClient extends HttpClient {
    def send(httpRequest: HttpRequest)(implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[HttpResponse] = {
      Future(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, csv)))(executionContext)
    }
  }

  val amendRegionPortTerminalcsv =
    """Region,Port,Terminal,IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |Heathrow,LHR,T1,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T1,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T2,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T2,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T3,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T3,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T4,EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |Heathrow,LHR,T4,SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

  val csv =
    """IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages
      |EI0152,EI0152,DUB,/221R,On Chocks,2022-07-22 08:00,2022-07-22 08:01,2022-07-22 08:01,2022-07-22 08:09,2022-07-22 08:08,1,2022-07-22 08:14,94,-,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |SQ0306,SQ0306,SIN,/243,On Chocks,2022-07-22 07:45,2022-07-22 07:56,2022-07-22 07:56,2022-07-22 08:03,2022-07-22 08:03,11,2022-07-22 08:15,245,215,,,,,,,,,,,,,,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,"",""
      |""".stripMargin

}
