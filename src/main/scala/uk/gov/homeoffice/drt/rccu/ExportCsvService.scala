package uk.gov.homeoffice.drt.rccu

import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import akka.stream.Materializer
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.{ExecutionContextExecutor, Future}

class ExportCsvService(httpClient: HttpClient) {

  val log: Logger = LoggerFactory.getLogger(getClass)

  val drtExportCsvRoutePath = "export/arrivals"

  def getPortRegion(region: String) = PortRegion.regions.find(_.name == region)

  def getUri(portCode: String, start: String, end: String, terminal: String): String =
    s"${Dashboard.drtInternalUriForPortCode(portCode)}/$drtExportCsvRoutePath/$start/$end/$terminal"

  def getPortResponseForTerminal(start: String, end: String, regionName: String, port: String, terminal: String)
                                (implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[String] = {
    val uri = getUri(port, start, end, terminal)
    val httpRequest = httpClient.createPortArrivalImportRequest(uri, port)
    httpClient
      .send(httpRequest)
      .flatMap { r =>
        if (r.status == OK) {
          r.entity.dataBytes
            .runReduce(_ ++ _)
            .map {
              _
                .utf8String
                .split("\n")
                .filterNot(_.contains("ICAO"))
                .map(line => s"${regionName},$port,$terminal,$line")
                .mkString("\n")
            }
            .recover(e => {
              log.error(s"Error while requesting export for $uri", e)
              ""
            })
        } else {
          r.entity.discardBytes()
          log.warn(s"Not OK response $r")
          Future.successful("")
        }
      }.recoverWith {
      case e: Throwable =>
        log.error(s"Error while requesting drt for $uri", e)
        Future.successful("")
    }
  }

  val formattedStringDate: DateTime => String = dateTime => DateTimeFormat.forPattern("yyyyMMddHHmmss").print(dateTime)

  val getCurrentTimeString: () => String = () => formattedStringDate(DateTime.now())

  val stringToDate: String => DateTime = dateTimeString => DateTimeFormat.forPattern("yyyy-MM-dd")
    .withZone(DateTimeZone.forID("Europe/London"))
    .parseDateTime(dateTimeString)

  def makeFileName(start: String, end: String, portRegion: String): String = {
    val startDateTime: DateTime = stringToDate(start)
    val endDateTime: DateTime = stringToDate(end)
    val endDate = if (endDateTime.minusDays(1).isAfter(startDateTime))
      f"-to-${
        end
      }"
    else ""

    f"$portRegion-${getCurrentTimeString()}-" +
      f"$start" + endDate + ".csv"
  }
}
