package uk.gov.homeoffice.drt.rccu

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.{ IOResult, Materializer }
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.ports.{ PortCode, PortRegion }
import uk.gov.homeoffice.drt.routes.ExportRoutes.{ exportCsvService, getPortRegion }
import uk.gov.homeoffice.drt.{ Dashboard, HttpClient }

import java.nio.file.Paths
import java.nio.file.StandardOpenOption.{ APPEND, CREATE, WRITE }
import scala.concurrent.{ ExecutionContextExecutor, Future }

case class PortResponse(port: PortCode, regionName: String, terminal: String, httpResponse: Option[HttpResponse])

class ExportCsvService(httpClient: HttpClient) {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def drtUriForPortCode(portCode: String): String = s"http://${portCode.toLowerCase}:9000"

  val drtExportCsvRoutePath = "export/arrivals"

  def getTerminal(portCode: String): List[String] = portCode match {
    case "BHX" => List("T1", "T2")
    case "EDI" => List("A1", "A2")
    case "LGW" => List("N", "S")
    case "LHR" => List("T1", "T2", "T3", "T4")
    case "MAN" => List("T1", "T2", "T3")
    case _ => List("T1")
  }

  def getUri(portCode: String, start: String, end: String, terminal: String): String =
    s"${Dashboard.drtUriForPortCode(portCode)}/${drtExportCsvRoutePath}/$start/$end/$terminal"

  def createFileWithHeader(fileName: String, startDate: String, endDate: String, region: String)(implicit ec: ExecutionContextExecutor, mat: Materializer): Future[Set[IOResult]] = {
    val prF: Future[Set[PortResponse]] = Future.sequence(exportCsvService.getPortResponseForRegionPorts(startDate, endDate, getPortRegion(region)))
      .map(_.filter(_.httpResponse.isDefined))
    prF.map(_.zipWithIndex.map {
      case (pr: PortResponse, i: Int) =>
        if (i == 0) exportCsvService.getCsvDataRegionPort(pr, fileName, true)
        else exportCsvService.getCsvDataRegionPort(pr, fileName, false)
    }).map(_.flatten).flatMap(Future.sequence(_))
  }

  def getCsvDataRegionPort(portResponse: PortResponse, fileName: String, keepHeader: Boolean)(implicit executionContext: ExecutionContextExecutor, mat: Materializer) = {
    val file = Paths.get(s"$fileName")
    portResponse.httpResponse.map { response =>
      response.entity.dataBytes
        .via(CsvParsing.lineScanner().map { line =>
          if (line.map(_.utf8String).contains("ICAO")) {
            ByteString(s"Region") :: ByteString(s"Port") :: ByteString(s"Terminal") :: line
          } else {
            ByteString(s"${portResponse.regionName}") :: ByteString(s"${portResponse.port.iata}") :: ByteString(s"${portResponse.terminal}") :: line
          }
        }).filterNot(a => !keepHeader && a.map(_.utf8String).contains("ICAO"))
        .map(a => ByteString(a.map(_.utf8String).mkString(",") + "\n"))
        .runWith(FileIO.toPath(file, options = Set(WRITE, APPEND, CREATE)))
    }
  }

  def getPortResponseForRegionPorts(start: String, end: String, portRegion: PortRegion)(implicit executionContext: ExecutionContextExecutor, mat: Materializer) = {
    portRegion.ports.flatMap {
      port =>
        getTerminal(port.iata).map { terminal =>
          val uri = getUri(port.iata, start, end, terminal)
          val httpRequest = httpClient.createPortArrivalImportRequest(uri, port.iata)
          httpClient.send(httpRequest)
            .map(r => if (r.status == OK)
              PortResponse(port, portRegion.name, terminal, Option(r))
            else {
              log.warn(s"Not OK response $r")
              r.discardEntityBytes()
              PortResponse(port, portRegion.name, terminal, None)
            }).recoverWith {
              case e: Throwable =>
                log.error(s"Error while requesting drt for $uri", e)
                Future.successful(PortResponse(port, portRegion.name, terminal, None))
            }
        }
    }
  }
}
