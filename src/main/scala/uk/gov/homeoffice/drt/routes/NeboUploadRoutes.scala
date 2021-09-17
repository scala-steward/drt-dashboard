package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes.{ Forbidden, InternalServerError, MethodNotAllowed }
import akka.http.scaladsl.server.Directives.{ complete, fileUpload, onSuccess }
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.Materializer
import akka.stream.scaladsl.{ Framing, Source }
import akka.util.ByteString
import com.github.tototoshi.csv._
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.slf4j.{ Logger, LoggerFactory }
import spray.json._
import uk.gov.homeoffice.drt.Dashboard._
import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.NeboUpload
import uk.gov.homeoffice.drt.routes.ApiRoutes.authByRole
import uk.gov.homeoffice.drt.{ HttpClient, JsonSupport }

import scala.concurrent.{ ExecutionContextExecutor, Future }

case class Row(urnReference: String, associatedText: String, flightCode: String, arrivalPort: String, arrivalDate: String, arrivalTime: String, departureDate: Option[String], departureTime: Option[String], embarkPort: Option[String], departurePort: Option[String])

case class FlightData(portCode: String, flightCode: String, scheduled: Long, scheduledDeparture: Option[Long], departurePort: Option[String], embarkPort: Option[String], paxCount: Int)

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
      authByRole(NeboUpload) {
        fileUploadCSV(neboPortCodes, httpClient)
      })

  def fileUploadCSV(neboPortCodes: List[String], httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Route = {
    fileUpload("csv") {
      case (metadata, byteSource) =>
        println(s"got here")
        onSuccess(
          Future.sequence(
            neboPortCodes
              .map(sendFlightDataToPort(
                convertByteSourceToFlightData(metadata, byteSource), _, httpClient))))(feedStatus => complete(feedStatus.toJson))
    }
  }

  def sendFlightDataToPort(flightData: Future[List[FlightData]], portCode: String, httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Future[FeedStatus] = {
    flightData.flatMap { fd =>
      val filterPortFlight = fd.filter(_.portCode.toLowerCase == portCode.toLowerCase)
      val httpRequest = httpClient.createDrtNeboRequest(
        filterPortFlight, s"${drtUriForPortCode(portCode)}$drtRoutePath", Roles.parse(portCode))
      httpClient.send(httpRequest)
        .map(r => FeedStatus(portCode, filterPortFlight.size, r.status.toString()))
    }
  }

  def convertByteSourceToFlightData(metadata: FileInfo, byteSource: Source[ByteString, Any])(implicit ec: ExecutionContextExecutor, mat: Materializer): Future[List[FlightData]] = {
    byteSource.via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 2048, allowTruncation = true))
      .map(convertByteStringToRow)
      .runFold(List.empty[Row]) { (r, n) => r :+ n }
      .map(rowToJson(_, metadata))
  }

  private def convertByteStringToRow(content: ByteString) = {
    val indexMapRow: Map[Int, String] = CSVParser.parse(content.utf8String, '\\', ',', '"')
      .map(_.zipWithIndex.map {
        case (k, v) => v -> k
      }.toMap)
      .getOrElse(Map.empty[Int, String])
    Row(
      urnReference = indexMapRow.getOrElse(0, ""),
      associatedText = indexMapRow.getOrElse(1, ""),
      flightCode = indexMapRow.getOrElse(2, ""),
      arrivalPort = indexMapRow.getOrElse(3, ""),
      arrivalDate = indexMapRow.getOrElse(4, ""),
      arrivalTime = indexMapRow.getOrElse(5, ""),
      departureDate = maybeColumnContent(indexMapRow.getOrElse(6, "")),
      departureTime = maybeColumnContent(indexMapRow.getOrElse(7, "")),
      embarkPort = maybeColumnContent(indexMapRow.getOrElse(8, "")),
      departurePort = maybeColumnContent(indexMapRow.getOrElse(9, "")))
  }

  val maybeColumnContent: String => Option[String] = column => if (column.isEmpty) None else Option(column)

  private def rowToJson(rows: List[Row], metadata: FileInfo): List[FlightData] = {
    val dataRows: Seq[Row] = rows.filterNot(_.flightCode.isEmpty).filterNot(_.flightCode.contains("Flight Code"))
    log.info(s"Processing ${dataRows.size} rows from the file name `${metadata.fileName}`")
    dataRows.groupBy(_.arrivalPort)
      .flatMap {
        case (arrivalPort, portRows) =>
          portRows.groupBy(_.flightCode)
            .flatMap {
              case (flightCode, flightRows) =>
                flightRows.groupBy(a => s"${a.arrivalDate} ${a.arrivalTime}").map {
                  case (arrivalDateTime, flightRowsByArrival) =>
                    FlightData(
                      portCode = arrivalPort,
                      flightCode = flightCode,
                      scheduled = parseDateToMillis(arrivalDateTime),
                      scheduledDeparture = flightRowsByArrival.head.departureDate.flatMap(dd => flightRowsByArrival.head.departureTime.map(dt => parseDateToMillis(s"$dd $dt"))),
                      departurePort = flightRowsByArrival.head.departurePort.map(_.trim),
                      embarkPort = flightRowsByArrival.head.embarkPort.map(_.trim),
                      flightRowsByArrival.size)
                }
            }
      }.toList
  }

  val parseDateToMillis: String => MillisSinceEpoch = date =>
    DateTimeFormat.forPattern("dd/MM/yyyy HH:mm").withZone(DateTimeZone.forID("Europe/London")).parseDateTime(date).getMillis

}
