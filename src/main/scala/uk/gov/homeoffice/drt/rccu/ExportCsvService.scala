package uk.gov.homeoffice.drt.rccu

import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.Materializer
import akka.util.ByteString
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.time.SDateLike
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.{ExecutionContextExecutor, Future}

class ExportCsvService(httpClient: HttpClient) {

  val log: Logger = LoggerFactory.getLogger(getClass)

  private val drtExportCsvRoutePath = "export/arrivals"

  def getPortRegion(region: String): Option[PortRegion] = PortRegion.regions.find(_.name == region)

  def getUri(portCode: String, start: String, end: String, terminal: String): String =
    s"${Dashboard.drtInternalUriForPortCode(portCode)}/$drtExportCsvRoutePath/$start/$end/$terminal"

  def getPortResponseForTerminal(start: String, end: String, regionName: String, port: String, terminal: String)
                                (implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[ByteString] = {
    val uri = getUri(port, start, end, terminal)
    val httpRequest = httpClient.createPortArrivalImportRequest(uri, port)
    httpClient
      .send(httpRequest)
      .flatMap { r =>
        if (r.status == OK) {
          log.info(s"Got 200 response from $uri")
          r.entity.dataBytes
            .runReduce(_ ++ _)
            .map { chunk =>
              ByteString(chunk
                .utf8String
                .split("\n")
                .filterNot(_.contains("ICAO"))
                .map(line => s"$regionName,$port,$terminal," + line)
                .mkString("\n")
              )
            }
            .recover { case e: Throwable =>
              log.error(s"Error while requesting export for $uri", e)
              ByteString("")
            }
        }
        else {
          r.entity.discardBytes()
          log.error(s"Got non-200 response ${r.status} from $uri")
          Future.successful(ByteString(""))
        }
      }
      .recoverWith {
        case e: Throwable =>
          log.error(s"Error while requesting export for $uri", e)
          Future.successful(ByteString(""))
      }
  }

  private val stringToDate: String => DateTime = dateTimeString => DateTimeFormat.forPattern("yyyy-MM-dd")
    .withZone(DateTimeZone.forID("Europe/London"))
    .parseDateTime(dateTimeString)

  def makeFileName(start: String, end: String, portRegion: String, createdAt: SDateLike): String = {
    val startDateTime: DateTime = stringToDate(start)
    val endDateTime: DateTime = stringToDate(end)
    val endDate = if (endDateTime.minusDays(1).isAfter(startDateTime))
      f"-to-$end"
    else ""

    val timestamp = f"${createdAt.getFullYear}${createdAt.getMonth}%02d${createdAt.getDate}%02d${createdAt.getHours}%02d${createdAt.getMinutes}%02d${createdAt.getSeconds}%02d"

    s"$portRegion-$timestamp-$start$endDate.csv"
  }
}
