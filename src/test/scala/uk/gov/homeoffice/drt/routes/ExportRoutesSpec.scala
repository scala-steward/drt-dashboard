package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.{Done, NotUsed}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.ArrivalExportHeadings
import uk.gov.homeoffice.drt.routes.ExportRoutes.RegionExportRequest
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

import scala.concurrent.{ExecutionContextExecutor, Future}


class ExportRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  implicit val csvStreaming: CsvEntityStreamingSupport = EntityStreamingSupport.csv()//.withFramingRenderer(Flow[ByteString])

  val csv: String =
    """IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Predicted Arrival,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track
      |flight1,information,row
      |flight2,information,row
      |""".stripMargin

  def httpResponse(content: String): HttpResponse = HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, Source(Seq(ByteString(content)))))

  val mockHttpClient: MockHttpClient = MockHttpClient(httpResponse(csv))
  val uploadProbe: TestProbe[(String, String)] = TestProbe[(String, String)]()
  val downloadProbe: TestProbe[String] = TestProbe[String]()
  val mockUploader: (String, Source[ByteString, Any]) => Future[Done.type] = (objectKey: String, data: Source[ByteString, Any]) =>
    data
      .runReduce[ByteString](_ ++ ByteString("\n") ++ _).map { bytes =>
      uploadProbe.ref ! (objectKey, bytes.utf8String)
      Done
    }
  val mockDownloader: String => Future[Source[ByteString, NotUsed]] = (objectKey: String) => {
    downloadProbe.ref ! objectKey
    Future.successful(Source(Seq(ByteString("1"), ByteString("2"), ByteString("3"))))
  }
  val now: SDateLike = SDate("2022-08-02T00:00:00")
  val nowYYYYMMDDHHmmss = "20220802000000"
  val nowProvider: () => SDateLike = () => now

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import uk.gov.homeoffice.drt.json.RegionExportJsonFormats._

  "Request heathrow arrival export" should {
    "collate all terminal arrivals" in {
      val request = RegionExportRequest("Heathrow", LocalDate(2022, 8, 2), LocalDate(2022, 8, 3))
      Post("/export", request) ~> RawHeader("X-Auth-Email", "someone@somwehere.com") ~> ExportRoutes(mockHttpClient, mockUploader, mockDownloader, nowProvider) ~> check {
        uploadProbe.expectMessage((s"Heathrow-$nowYYYYMMDDHHmmss-2022-08-02-to-2022-08-03.csv", heathrowRegionPortTerminalData.trim))
        responseAs[String] should ===("ok")
      }
    }
  }

  //  "Request North arrival export" should {
  //    "collate all port and terminal arrivals in the North region" in {
  //      val request = RegionExportRequest("North", LocalDate(2022, 8, 2), LocalDate(2022, 8, 3))
  //      Post("/export/North/2022-08-02/2022-08-03", request) ~> ExportRoutes(mockHttpClient, mockUploader, mockDownloader, nowProvider) ~> check {
  //        responseAs[String] should ===(northRegionPortTerminalData)
  //      }
  //    }
  //  }
  //
  //  "Request South arrival export" should {
  //    "collate all port and terminal arrivals in the South region" in {
  //      val request = RegionExportRequest("South", LocalDate(2022, 8, 2), LocalDate(2022, 8, 3))
  //      Post("/export/South/2022-08-02/2022-08-03", request) ~> ExportRoutes(mockHttpClient, mockUploader, mockDownloader, nowProvider) ~> check {
  //        responseAs[String] should ===(southRegionPortTerminalData)
  //      }
  //    }
  //  }
  //
  //  "Request Central arrival export" should {
  //    "collate all port and terminal arrivals in the Central region" in {
  //      val request = RegionExportRequest("Central", LocalDate(2022, 8, 2), LocalDate(2022, 8, 3))
  //      Post("/export/Central/2022-08-02/2022-08-03", request) ~> ExportRoutes(mockHttpClient, mockUploader, mockDownloader, nowProvider) ~> check {
  //        responseAs[String] should ===(centralRegionPortTerminalData)
  //      }
  //    }
  //  }

  def heathrowRegionPortTerminalData: String =
    s"""${ArrivalExportHeadings.regionalExportHeadings}
       |Heathrow,LHR,T2,flight1,information,row
       |Heathrow,LHR,T2,flight2,information,row
       |Heathrow,LHR,T3,flight1,information,row
       |Heathrow,LHR,T3,flight2,information,row
       |Heathrow,LHR,T4,flight1,information,row
       |Heathrow,LHR,T4,flight2,information,row
       |Heathrow,LHR,T5,flight1,information,row
       |Heathrow,LHR,T5,flight2,information,row
       |""".stripMargin

  def northRegionPortTerminalData: String =
    s"""${ArrivalExportHeadings.regionalExportHeadings}
       |North,ABZ,T1,flight1,information,row
       |North,ABZ,T1,flight2,information,row
       |North,BFS,T1,flight1,information,row
       |North,BFS,T1,flight2,information,row
       |North,BHD,T1,flight1,information,row
       |North,BHD,T1,flight2,information,row
       |North,DSA,T1,flight1,information,row
       |North,DSA,T1,flight2,information,row
       |North,EDI,A1,flight1,information,row
       |North,EDI,A1,flight2,information,row
       |North,EDI,A2,flight1,information,row
       |North,EDI,A2,flight2,information,row
       |North,GLA,T1,flight1,information,row
       |North,GLA,T1,flight2,information,row
       |North,HUY,T1,flight1,information,row
       |North,HUY,T1,flight2,information,row
       |North,INV,T1,flight1,information,row
       |North,INV,T1,flight2,information,row
       |North,LBA,T1,flight1,information,row
       |North,LBA,T1,flight2,information,row
       |North,LPL,T1,flight1,information,row
       |North,LPL,T1,flight2,information,row
       |North,MAN,T1,flight1,information,row
       |North,MAN,T1,flight2,information,row
       |North,MAN,T2,flight1,information,row
       |North,MAN,T2,flight2,information,row
       |North,MAN,T3,flight1,information,row
       |North,MAN,T3,flight2,information,row
       |North,MME,T1,flight1,information,row
       |North,MME,T1,flight2,information,row
       |North,NCL,T1,flight1,information,row
       |North,NCL,T1,flight2,information,row
       |North,PIK,T1,flight1,information,row
       |North,PIK,T1,flight2,information,row
       |""".stripMargin

  def southRegionPortTerminalData: String =
    s"""${ArrivalExportHeadings.regionalExportHeadings}
       |South,BOH,T1,flight1,information,row
       |South,BOH,T1,flight2,information,row
       |South,BRS,T1,flight1,information,row
       |South,BRS,T1,flight2,information,row
       |South,CWL,T1,flight1,information,row
       |South,CWL,T1,flight2,information,row
       |South,EXT,T1,flight1,information,row
       |South,EXT,T1,flight2,information,row
       |South,LGW,N,flight1,information,row
       |South,LGW,N,flight2,information,row
       |South,LGW,S,flight1,information,row
       |South,LGW,S,flight2,information,row
       |South,NQY,T1,flight1,information,row
       |South,NQY,T1,flight2,information,row
       |South,SOU,T1,flight1,information,row
       |South,SOU,T1,flight2,information,row
       |""".stripMargin

  def centralRegionPortTerminalData: String =
    s"""${ArrivalExportHeadings.regionalExportHeadings}
       |Central,BHX,T1,flight1,information,row
       |Central,BHX,T1,flight2,information,row
       |Central,BHX,T2,flight1,information,row
       |Central,BHX,T2,flight2,information,row
       |Central,EMA,T1,flight1,information,row
       |Central,EMA,T1,flight2,information,row
       |Central,LCY,T1,flight1,information,row
       |Central,LCY,T1,flight2,information,row
       |Central,LTN,T1,flight1,information,row
       |Central,LTN,T1,flight2,information,row
       |Central,NWI,T1,flight1,information,row
       |Central,NWI,T1,flight2,information,row
       |Central,SEN,T1,flight1,information,row
       |Central,SEN,T1,flight2,information,row
       |Central,STN,T1,flight1,information,row
       |Central,STN,T1,flight2,information,row
       |""".stripMargin

}
