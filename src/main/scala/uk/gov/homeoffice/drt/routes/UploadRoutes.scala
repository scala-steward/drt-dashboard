package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes.{ Forbidden, InternalServerError, MethodNotAllowed }
import akka.http.scaladsl.server.Directives.{ complete, fileUpload, onSuccess, pathPrefix, post, _ }
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.server.{ Route, _ }
import akka.stream.Materializer
import akka.stream.scaladsl.{ Framing, Source }
import akka.util.ByteString
import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone }
import org.slf4j.{ Logger, LoggerFactory }
import spray.json._
import uk.gov.homeoffice.drt.Dashboard._
import uk.gov.homeoffice.drt.auth.Roles.NeboUpload
import uk.gov.homeoffice.drt.routes.ApiRoutes.authByRole
import uk.gov.homeoffice.drt.routes.UploadRoutes.MillisSinceEpoch
import uk.gov.homeoffice.drt.{ HttpClient, JsonSupport }

import scala.concurrent.{ ExecutionContextExecutor, Future }

case class Row(urnReference: String, associatedText: String, flightCode: String, arrivalPort: String, date: String)

case class FlightData(portCode: String, flightCode: String, scheduled: MillisSinceEpoch, paxCount: Int)

case class FeedStatus(portCode: String, flightCount: Int, statusCode: String)

object UploadRoutes extends JsonSupport {

  type MillisSinceEpoch = Long

  val log: Logger = LoggerFactory.getLogger(getClass)

  val drtRoutePath = "/data/feeds/red-list-counts"

  implicit def rejectionHandler =
    RejectionHandler.newBuilder()
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
      .handleNotFound {
        complete("Not found!")
      }
      .result()

  def apply(prefix: String, neboPortCodes: List[String], httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Route = {
    val route: Route =
      pathPrefix(prefix) {
        authByRole(NeboUpload) {
          post {
            fileUploadCSV(neboPortCodes, httpClient)
          }
        }
      }

    handleRejections(rejectionHandler)(route)
  }

  def fileUploadCSV(neboPortCodes: List[String], httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Route = {
    fileUpload("csv") {
      case (metadata, byteSource) =>
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
        filterPortFlight, s"${drtUriForPortCode(portCode)}$drtRoutePath")
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

  private def convertByteStringToRow(byteString: ByteString): Row = {
    val indexMapRow: Map[Int, String] = byteString.utf8String.split(",")
      .zipWithIndex
      .toMap
      .map { case (k, v) => v -> k }

    Row(
      indexMapRow.getOrElse(0, "").trim,
      indexMapRow.getOrElse(1, "").trim,
      indexMapRow.getOrElse(2, "").trim,
      indexMapRow.getOrElse(3, "").trim,
      indexMapRow.getOrElse(4, "").trim)
  }

  private def rowToJson(rows: List[Row], metadata: FileInfo): List[FlightData] = {
    val dataRows: Seq[Row] = rows.tail.filterNot(_.flightCode.isEmpty)
    log.info(s"Processing ${dataRows.size} rows from the file name `${metadata.fileName}`")
    dataRows.groupBy(_.arrivalPort)
      .flatMap {
        case (arrivalPort, portRows) =>
          portRows.groupBy(_.flightCode)
            .map {
              case (flightCode, flightRows) =>
                FlightData(arrivalPort, flightCode, covertDateTime(flightRows.head.date), flightRows.size)
            }
      }.toList
  }

  val covertDateTime: String => MillisSinceEpoch = date => if (date.isEmpty) 0 else DateTime
    .parse(date, DateTimeFormat.forPattern("dd/MM/yyyy HH:mm:ss"))
    .withZone(DateTimeZone.UTC)
    .getMillis

}
