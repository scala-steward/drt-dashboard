package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes.{Forbidden, InternalServerError, MethodNotAllowed}
import akka.http.scaladsl.server.Directives.{complete, fileUpload, onSuccess, path}
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.Materializer
import com.github.tototoshi.csv._
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.slf4j.{Logger, LoggerFactory}
import spray.json._
import uk.gov.homeoffice.drt.Dashboard._
import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.NeboUpload
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.ApiRoutes.authByRole
import uk.gov.homeoffice.drt.{HttpClient, JsonSupport}

import scala.concurrent.{ExecutionContextExecutor, Future}

case class Row(flightCode: String, arrivalPort: String, arrivalDate: String, arrivalTime: String, departureDate: Option[String], departureTime: Option[String], embarkPort: Option[String], departurePort: Option[String], urn: String)

object Row {
  val flightCode = "flight code"
  val arrivalPort = "arrival port"
  val arrivalDate = "date"
  val arrivalTime = "arrival time"
  val embarkingPort = "embark port"
  val departurePort = "departure port"
  val departureDate = "departure date"
  val departureTime = "departure time"
  val urn = "reference  urn "
}

case class FlightData(portCode: String, flightCode: String, scheduled: Long, scheduledDeparture: Option[Long], departurePort: Option[String], embarkPort: Option[String], urns: Seq[String])

case class FeedStatus(portCode: String, flightCount: Int, statusCode: String)

case class NeboUploadRoutes(neboPortCodes: List[String], httpClient: HttpClient) extends JsonSupport {

  val routePrefix = "uploadFile"

  type MillisSinceEpoch = Long

  val log: Logger = LoggerFactory.getLogger(getClass)

  val drtRoutePath = "/data/feed/red-list-counts"

  implicit def rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle {
      case AuthorizationFailedRejection =>
        complete(Forbidden, "You are not authorized to upload!")
    }
    .handle {
      case ValidationRejection(msg, _) =>
        complete(InternalServerError, "Not valid data!" + msg)
    }
    .handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete(MethodNotAllowed, s"Not supported: ${names mkString " or "}!")
    }
    .result()

  def route(implicit ec: ExecutionContextExecutor, mat: Materializer): Route =
    Route.seal(
      (path("nebo-upload") & authByRole(NeboUpload)) {
        fileUploadCSV(neboPortCodes, httpClient)
      })

  def fileUploadCSV(neboPortCodes: List[String], httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Route = {
    fileUpload("csv") {
      case (metadata, byteSource) =>
        onSuccess(
          feedStatusForPortCode(neboPortCodes, convertByteSourceToFlightData(metadata, byteSource.runFold("")(_ ++ _.utf8String)), httpClient)) { fsl => complete(fsl.toJson) }
    }
  }

  def feedStatusForPortCode(neboPortCodes: List[String], flightData: Future[List[FlightData]], httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Future[List[FeedStatus]] =
    flightData.flatMap { fd =>
      Future.sequence(
        neboPortCodes.flatMap { portCode =>
          val filterPortFlight = fd.filter(f => f.portCode.toLowerCase == portCode.toLowerCase)
          if (filterPortFlight.isEmpty) {
            log.info(s"No nebo passenger details for flights from port $portCode")
            None
          } else Option(sendFlightDataToPort(filterPortFlight, portCode, httpClient))
        })
    }

  def sendFlightDataToPort(flightData: List[FlightData], portCode: String, httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Future[FeedStatus] = {
    val httpRequest = httpClient.createDrtNeboRequest(flightData, s"${drtInternalUriForPortCode(PortCode(portCode))}$drtRoutePath", Roles.parse(portCode))
    httpClient.send(httpRequest)
      .map(r => FeedStatus(portCode, flightData.size, r.status.toString()))
  }

  object CsvFormat extends CSVFormat {
    override val delimiter: Char = ','
    override val quoteChar: Char = '"'
    override val escapeChar: Char = '\\'
    override val lineTerminator: String = "\n"
    override val quoting: Quoting = QUOTE_MINIMAL
    override val treatEmptyLineAsNil: Boolean = false
  }

  val tidyHeader: String => String = c => c.trim.replaceAll("[^a-zA-Z0-9 ]+", " ").toLowerCase

  def convertByteSourceToFlightData(metadata: FileInfo, csvContent: Future[String])(implicit ec: ExecutionContextExecutor, mat: Materializer): Future[List[FlightData]] = {
    csvContent
      .map { content =>
        val reader = CSVReader.open(scala.io.Source.fromString(content))(CsvFormat)
        reader
          .allWithHeaders()
          .map(maybeFieldsToRow)
          .collect { case Some(row) => row }
      }
      .map { rows =>
        log.info(s"Processing ${rows.length} rows from the file name `${metadata.fileName}`")
        rowsToJson(rows)
      }
  }

  def maybeFieldsToRow(row: Map[String, String]): Option[Row] = {
    val tidied = row.map {
      case (header, cell) => (tidyHeader(header), cell)
    }
    (tidied.get(Row.flightCode), tidied.get(Row.arrivalPort), tidied.get(Row.arrivalDate), tidied.get(Row.arrivalTime)) match {
      case (Some(flightCode), Some(arrivalPort), Some(arrivalDate), Some(arrivalTime)) =>
        Option(Row(
          flightCode = flightCode,
          arrivalPort = arrivalPort,
          arrivalDate = arrivalDate,
          arrivalTime = arrivalTime,
          departureDate = tidied.get(Row.departureDate),
          departureTime = tidied.get(Row.departureTime),
          embarkPort = tidied.get(Row.embarkingPort),
          departurePort = tidied.get(Row.departurePort),
          urn = tidied.getOrElse(Row.urn, "")))

      case (fc, ap, ad, at) =>
        log.warn(s"Missing some fields: flight code: $fc, arrival port: $ap, arrival date: $ad, arrival time: $at.")
        None
    }
  }

  private def rowsToJson(rows: List[Row]): List[FlightData] = {
    val dataRows: Seq[Row] = rows.filterNot(_.flightCode.isEmpty).filterNot(_.flightCode.contains("Flight Code"))
    dataRows.groupBy(_.arrivalPort)
      .flatMap {
        case (arrivalPort, portRows) =>
          portRows.groupBy(_.flightCode)
            .flatMap {
              case (flightCode, flightRows) =>
                flightRows.groupBy(a => s"${a.arrivalDate} ${a.arrivalTime}")
                  .map {
                    case (arrivalDateTime, flightRowsByArrival) =>
                      FlightData(
                        portCode = arrivalPort,
                        flightCode = flightCode,
                        scheduled = parseDateToMillis(arrivalDateTime),
                        scheduledDeparture = flightRowsByArrival.head.departureDate.flatMap(dd => flightRowsByArrival.head.departureTime.map(dt => parseDateToMillis(s"$dd $dt"))),
                        departurePort = flightRowsByArrival.head.departurePort.map(_.trim),
                        embarkPort = flightRowsByArrival.head.embarkPort.map(_.trim),
                        urns = flightRowsByArrival.map(_.urn))
                  }
            }
      }.toList
  }

  val parseDateToMillis: String => MillisSinceEpoch = date =>
    DateTimeFormat.forPattern("dd/MM/yyyy HH:mm").withZone(DateTimeZone.forID("Europe/London")).parseDateTime(date).getMillis

}
