package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.arrivals.ArrivalExportHeadings

import scala.concurrent.ExecutionContextExecutor


class ExportRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  implicit val streamingSupport: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withContentType(ContentTypes.`text/csv(UTF-8)`)

  val csv: String =
    """IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Predicted Arrival,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track
      |flight1,information,row
      |flight2,information,row
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
    "collate all port and terminal arrivals in the South region" in {
      Get("/export/South/2022-08-02/2022-08-03") ~> ExportRoutes(mockHttpClient) ~> check {
        val a = responseAs[String]
        a should ===(southRegionPortTerminalData)
      }
    }
  }

  "Request Central arrival export" should {
    "collate all port and terminal arrivals in the Central region" in {
      Get("/export/Central/2022-08-02/2022-08-03") ~> ExportRoutes(mockHttpClient) ~> check {
        val a = responseAs[String]
        a should ===(centralRegionPortTerminalData)
      }
    }
  }

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
