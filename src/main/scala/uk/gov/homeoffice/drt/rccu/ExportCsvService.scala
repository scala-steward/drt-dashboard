package uk.gov.homeoffice.drt.rccu

import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.Materializer
import akka.util.ByteString
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.exports.ExportType
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.{ExecutionContextExecutor, Future}

case class ExportCsvService(httpClient: HttpClient) {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def getPortRegion(region: String): Option[PortRegion] = PortRegion.regions.find(_.name == region)

  def getUri(exportType: ExportType, start: LocalDate, end: LocalDate, portCode: PortCode, terminal: Terminal): String =
    s"${Dashboard.drtInternalUriForPortCode(portCode)}/${exportType.routePrefix}/$start/$end/$terminal"

  def getPortResponseForTerminal(exportType: ExportType, start: LocalDate, end: LocalDate, portCode: PortCode, terminal: Terminal)
                                (implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[ByteString] = {
    val uri = getUri(exportType, start, end, portCode, terminal)
    val httpRequest = httpClient.createPortArrivalImportRequest(uri, portCode)
    httpClient
      .send(httpRequest)
      .flatMap { r =>
        if (r.status == OK) {
          log.info(s"Got 200 response from $uri")
          r.entity.dataBytes
            .runReduce(_ ++ _)
            .map(content => ByteString(content.utf8String))
            .recover { case e: Throwable =>
              log.error(s"Error while requesting export for $uri", e)
              throw new Exception(s"Error while requesting export for $uri", e)
            }
        }
        else {
          r.entity.discardBytes()
          throw new Exception(s"Got non-200 response ${r.status} from $uri")
        }
      }
  }

  def makeFileName(start: String, end: String, portRegion: String, createdAt: SDateLike): String = {
    val endDate = if (start != end)
      f"-to-$end"
    else ""

    val timestamp = f"${createdAt.getFullYear}${createdAt.getMonth}%02d${createdAt.getDate}%02d${createdAt.getHours}%02d${createdAt.getMinutes}%02d${createdAt.getSeconds}%02d"

    s"$portRegion-$timestamp-$start$endDate.csv"
  }
}
