package uk.gov.homeoffice.drt.routes

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.slf4j.LoggerFactory
import spray.json.enrichAny
import uk.gov.homeoffice.drt.models.{PassengersSummaries, PassengersSummary}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion, Queues}
import uk.gov.homeoffice.drt.services.PassengerSummaryStreams
import uk.gov.homeoffice.drt.services.PassengerSummaryStreams.{Granularity, Total}
import uk.gov.homeoffice.drt.time.TimeZoneHelper.europeLondonTimeZone
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import scala.concurrent.ExecutionContext
import scala.util.Success


object PassengerRoutes {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(passengerSummaries: PassengerSummaryStreams)
           (implicit ec: ExecutionContext, mat: Materializer): Route =
    pathPrefix("passengers" / Segment / Segment) {
      case (startDate, endDate) =>
        parameters("port-codes") { portCodesStr =>
          val portCodes = portCodesStr.split(",")
          concat(
            pathEnd(
              passengersForPort(portCodes, startDate, endDate, None, passengerSummaries)
            ),
            path(Segment)(terminal =>
              passengersForPort(portCodes, startDate, endDate, Some(terminal), passengerSummaries)
            )
          )
        }
    }

  private def passengersForPort(portCodes: Iterable[String],
                                startDate: String,
                                endDate: String,
                                maybeTerminal: Option[String],
                                passengerSummaries: PassengerSummaryStreams,
                               )
                               (implicit ec: ExecutionContext, mat: Materializer): Route = {
    get {
      extractRequest { request =>
        parameters("granularity".optional) { maybeGranularity =>
          val (start, end) = (LocalDate.parse(startDate), LocalDate.parse(endDate)) match {
            case (Some(s), Some(e)) => (s, e)
            case _ => throw new IllegalArgumentException(s"Invalid date range: $startDate - $endDate")
          }
          val granularity = maybeGranularity.map(Granularity.fromString).getOrElse(Total)
          val streamForPort = passengerSummaries.streamForGranularity(start, end, granularity, maybeTerminal.map(Terminal(_)))

          val contentType = request.headers.find(_.name() == "Accept").map {
            case header if header.value() == "text/csv" => ContentTypes.`text/csv(UTF-8)`
            case _ => ContentTypes.`application/json`
          }.getOrElse(ContentTypes.`application/json`)

          val portResult: Source[(PortCode, (Map[Queue, Int], Int, Option[Any])), NotUsed] = Source(portCodes.toList)
            .flatMapConcat(portCodeStr => {
              val portCode = PortCode(portCodeStr)
              streamForPort(portCode).map(result => (portCode, result))
            })

          val eventualContent = if (contentType == ContentTypes.`text/csv(UTF-8)`)
            portResult.runFold("") {
              case (acc, (portCode, (queues, capacity, x))) => acc + passengersCsvRow(portCode, maybeTerminal, queues, capacity, x)
            }
          else {
            import uk.gov.homeoffice.drt.jsonformats.PassengersSummaryFormat._
            portResult
              .runFold(PassengersSummaries.empty) {
                case (acc, (portCode, (queues, capacity, x))) => acc ++ Seq(passengersJson(portCode, maybeTerminal, queues, capacity, x))
              }
              .map(_.toJson.compactPrint)
          }

          onComplete(eventualContent) {
            case Success(content) => complete(HttpEntity(contentType, content))
          }
        }
      }
    }
  }

  private def passengersCsvRow[T]: (PortCode, Option[String], Map[Queue, Int], Int, Option[T]) => String =
    (portCode, maybeTerminal, queueCounts, capacity, maybeDateOrDateHour) => {
      val regionName = PortRegion.fromPort(portCode).name
      val portCodeStr = portCode.toString
      val totalPcpPax = queueCounts.values.sum
      val queueCells = Queues.queueOrder
        .map(queue => queueCounts.getOrElse(queue, 0).toString)
        .mkString(",")

      val dateStr = maybeDateOrDateHour.map {
        case date: LocalDate => date.toISOString
        case date: Long => s"${SDate(date, europeLondonTimeZone).toISOString}"
      }
      maybeTerminal match {
        case Some(terminal) =>
          (dateStr.toList ++ List(regionName, portCodeStr, terminal, capacity, totalPcpPax, queueCells)).mkString(",") + "\n"
        case None =>
          (dateStr.toList ++ List(regionName, portCodeStr, capacity, totalPcpPax, queueCells)).mkString(",") + "\n"
      }
    }

  private def passengersJson[T]: (PortCode, Option[String], Map[Queue, Int], Int, Option[T]) => PassengersSummary =
    (portCode, maybeTerminal, queueCounts, capacity, maybeDateOrDateHour) => {
      val regionName = PortRegion.fromPort(portCode).name
      val portCodeStr = portCode.toString
      val totalPcpPax = queueCounts.values.sum
      val (maybeDate, maybeHour) = maybeDateOrDateHour match {
        case Some(date: LocalDate) => (Option(date), None)
        case Some(date: Long) =>
          val sdate = SDate(date, europeLondonTimeZone)
          (Option(sdate.toLocalDate), Option(sdate.getHours))
        case _ => (None, None)
      }
      PassengersSummary(regionName, portCodeStr, maybeTerminal, capacity, totalPcpPax, queueCounts, maybeDate, maybeHour)
    }
}
