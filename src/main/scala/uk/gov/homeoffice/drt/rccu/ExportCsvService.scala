package uk.gov.homeoffice.drt.rccu

import akka.http.scaladsl.model.{ContentType, HttpHeader, MediaTypes}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.{Accept, `Content-Type`}
import akka.stream.Materializer
import akka.util.ByteString
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.exports.{DailyExportType, ExportType}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

object ExportCsvService {
  def getUri(exportType: ExportType, start: LocalDate, end: LocalDate, portCode: PortCode, maybeTerminal: Option[Terminal]): String = {
    val granularity = exportType match {
      case _: DailyExportType => "daily"
      case _ => "total"
    }
    val terminalName = maybeTerminal match {
      case Some(terminal) => s"/$terminal"
      case None => ""
    }
    s"${Dashboard.drtInternalUriForPortCode(portCode)}/api/${exportType.routePrefix}/$start/$end$terminalName?granularity=$granularity"
  }
}

case class ExportCsvService(httpClient: HttpClient) {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def getPortResponseForTerminal(uri: String, portCode: PortCode)
                                (implicit executionContext: ExecutionContext, mat: Materializer): Future[ByteString] = {
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

  def makeFileName(start: String, end: String, createdAt: SDateLike): String = {
    val endDate = if (start != end)
      f"-to-$end"
    else ""

    val date = f"${createdAt.getFullYear}${createdAt.getMonth}%02d${createdAt.getDate}%02d"
    val milliseconds = createdAt.millisSinceEpoch.toString.takeRight(3)
    val timestamp = date + f"${createdAt.getHours}%02d${createdAt.getMinutes}%02d${createdAt.getSeconds}%02d.$milliseconds"

    s"$timestamp-$start$endDate.csv"
  }
}
