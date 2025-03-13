package uk.gov.homeoffice.drt.routes

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import spray.json.enrichAny
import uk.gov.homeoffice.drt.model.{PassengersSummaries, PassengersSummary}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion, Queues}
import uk.gov.homeoffice.drt.services.PassengerSummaryStreams.{Granularity, Total}
import uk.gov.homeoffice.drt.time.TimeZoneHelper.europeLondonTimeZone
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object PassengerRoutes {
  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def apply(summaryProvider: (LocalDate, LocalDate, Granularity, Option[Terminal]) => PortCode => Source[(Map[Queue, Int], Int, Map[Queue, Int], Option[Any]), NotUsed])
           (implicit ec: ExecutionContext, mat: Materializer): Route =
    pathPrefix("passengers" / Segment / Segment) {
      case (startDate, endDate) =>
        parameters("port-codes") { portCodesStr =>
          val portCodes = portCodesStr.split(",")
          concat(
            pathEnd(
              passengersForPort(portCodes, startDate, endDate, None, summaryProvider)
            ),
            path(Segment)(terminal =>
              passengersForPort(portCodes, startDate, endDate, Some(terminal), summaryProvider)
            )
          )
        }
    }

  private def passengersForPort(portCodes: Iterable[String],
                                startDate: String,
                                endDate: String,
                                maybeTerminal: Option[String],
                                summaryProvider: (LocalDate, LocalDate, Granularity, Option[Terminal]) => PortCode => Source[(Map[Queue, Int], Int, Map[Queue, Int], Option[Any]), NotUsed],
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
          val streamForPort = summaryProvider(start, end, granularity, maybeTerminal.map(Terminal(_)))

          val contentType = contentTypeFromRequest(request)

          val portResults = Source(portCodes.toList)
            .flatMapConcat { portCodeStr =>
              val portCode = PortCode(portCodeStr)
              streamForPort(portCode).map(result => (portCode, result))
            }

          val eventualContent = sourceToContent(contentType, portResults, maybeTerminal)

          onComplete(eventualContent) {
            case Success(content) => complete(HttpEntity(contentType, content))
            case Failure(t) =>
              log.error(s"Failed to get passengers for $portCodes, $startDate, $endDate, $maybeTerminal", t)
              complete(StatusCodes.InternalServerError, t.getMessage)
          }
        }
      }
    }
  }

  private def contentTypeFromRequest(request: HttpRequest) = {
    request.headers.find(_.name() == "Accept").map {
      case header if header.value() == "text/csv" => ContentTypes.`text/csv(UTF-8)`
      case _ => ContentTypes.`application/json`
    }.getOrElse(ContentTypes.`application/json`)
  }

  private def sourceToContent(contentType: ContentType,
                              portResult: Source[(PortCode, (Map[Queue, Int], Int, Map[Queue, Int], Option[Any])), NotUsed],
                              maybeTerminal: Option[String],
                             )
                             (implicit mat: Materializer, ec: ExecutionContext): Future[String] = {
    if (contentType == ContentTypes.`text/csv(UTF-8)`)
      portResult.runFold("") {
        case (acc, (portCode, (queues, capacity, bx, maybeDateOrDateHour))) => acc + passengersCsvRow(portCode, maybeTerminal, queues, capacity, bx, maybeDateOrDateHour)
      }
    else {
      import uk.gov.homeoffice.drt.jsonformats.PassengersSummaryFormat._
      portResult
        .runFold(PassengersSummaries.empty) {
          case (acc, (portCode, (queues, capacity, bx, x))) => acc ++ Seq(passengersJson(portCode, maybeTerminal, queues, capacity, bx, x))
        }
        .map(_.summaries.toJson.compactPrint)
    }
  }

  private def passengersCsvRow[T]: (PortCode, Option[String], Map[Queue, Int], Int, Map[Queue, Int], Option[T]) => String =
    (portCode, maybeTerminal, drtQueueCounts, capacity, bxQueueCounts, maybeDateOrDateHour) => {
      val regionName = PortRegion.fromPort(portCode).name
      val portCodeStr = portCode.toString
      val totalPcpPax = drtQueueCounts.values.sum
      val drtQueueCells = Queues.queueOrder
        .map(queue => drtQueueCounts.getOrElse(queue, 0).toString)
        .mkString(",")
      val bxQueueCells = Queues.queueOrder
        .map(queue => bxQueueCounts.getOrElse(queue, 0).toString)
        .mkString(",")

      val dateStr = maybeDateOrDateHour.map {
        case date: LocalDate => date.toISOString
        case date: Long => s"${SDate(date, europeLondonTimeZone).toISOString}"
      }
      maybeTerminal match {
        case Some(terminal) =>
          (dateStr.toList ++ List(regionName, portCodeStr, terminal, capacity, totalPcpPax, drtQueueCells, bxQueueCells)).mkString(",") + "\n"
        case None =>
          (dateStr.toList ++ List(regionName, portCodeStr, capacity, totalPcpPax, drtQueueCells, bxQueueCells)).mkString(",") + "\n"
      }
    }

  private def passengersJson[T]: (PortCode, Option[String], Map[Queue, Int], Int, Map[Queue, Int], Option[T]) => PassengersSummary =
    (portCode, maybeTerminal, drtQueueCounts, capacity, bxQueueCounts, maybeDateOrDateHour) => {
      val regionName = PortRegion.fromPort(portCode).name
      val portCodeStr = portCode.toString
      val (maybeDate, maybeHour) = maybeDateOrDateHour match {
        case Some(date: LocalDate) => (Option(date), None)
        case Some(date: Long) =>
          val sdate = SDate(date, europeLondonTimeZone)
          (Option(sdate.toLocalDate), Option(sdate.getHours))
        case _ => (None, None)
      }
      PassengersSummary(regionName, portCodeStr, maybeTerminal, capacity, drtQueueCounts, bxQueueCounts, maybeDate, maybeHour)
    }
}
