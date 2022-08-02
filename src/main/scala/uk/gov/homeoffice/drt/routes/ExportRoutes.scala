package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.ContentTypeResolver.Default
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.stream.Materializer
import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone }
import uk.gov.homeoffice.drt.ProdHttpClient
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.rccu.ExportCsvService

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success }

object ExportRoutes {

  lazy val exportCsvService = new ExportCsvService(new ProdHttpClient())

  def apply(fileStorePath: String)(implicit ec: ExecutionContextExecutor, mat: Materializer): Route =
    path("export" / Segment / Segment / Segment) { (region, startDate, endDate) =>
      val fileName = makeFileName(getCurrentTimeString, startDate, endDate, region)
      val fileAbsolutePath = s"$fileStorePath$fileName"
      onComplete(exportCsvService.createFileWithHeader(fileAbsolutePath, startDate, endDate, region)) {
        case Success(_) => getFromFile(fileAbsolutePath)
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }

  val formattedStringDate: DateTime => String = dateTime => DateTimeFormat.forPattern("yyyyMMddHHmmss").print(dateTime)

  def getCurrentTimeString = formattedStringDate(DateTime.now())

  def getPortRegion(region: String): PortRegion = PortRegion.regions.filter(_.name == region).head

  val stringToDate: String => DateTime = dateTimeString => DateTimeFormat.forPattern("yyyy-MM-dd")
    .withZone(DateTimeZone.forID("Europe/London"))
    .parseDateTime(dateTimeString)

  def makeFileName(subject: String, start: String, end: String, portRegion: String): String = {
    val startDateTime: DateTime = stringToDate(start)
    val endDateTime: DateTime = stringToDate(end)
    val endDate = if (endDateTime.minusDays(1).isAfter(startDateTime))
      f"-to-${end}"
    else ""

    f"$portRegion-$subject-" +
      f"$start" + endDate + ".csv"
  }

}
