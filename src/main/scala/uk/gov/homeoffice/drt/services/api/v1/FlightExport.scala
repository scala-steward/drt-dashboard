package uk.gov.homeoffice.drt.services.api.v1

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import uk.gov.homeoffice.drt.Server.paxFeedSourceOrder
import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, Arrival}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.routes.api.v1.FlightApiV1Routes.FlightJsonResponse
import uk.gov.homeoffice.drt.services.AirportInfoService
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object FlightExport {
  case class FlightJson(code: String,
                        originPortIata: String,
                        originPortName: String,
                        scheduledTime: Long,
                        estimatedLandingTime: Option[Long],
                        actualChocksTime: Option[Long],
                        estimatedPcpStartTime: Option[Long],
                        estimatedPcpEndTime: Option[Long],
                        estimatedPaxCount: Option[Int],
                        status: String,
                       )

  object FlightJson {
    def apply(ar: Arrival)
             (implicit sourceOrderPreference: List[FeedSource]): FlightJson = FlightJson(
      code = ar.flightCodeString,
      originPortIata = ar.Origin.iata,
      originPortName = AirportInfoService.airportInfo(ar.Origin).map(_.airportName).getOrElse("n/a"),
      scheduledTime = ar.Scheduled,
      estimatedLandingTime = ar.Estimated,
      actualChocksTime = ar.ActualChox,
      estimatedPcpStartTime = Try(ar.pcpRange(sourceOrderPreference).min).toOption,
      estimatedPcpEndTime = Try(ar.pcpRange(sourceOrderPreference).max).toOption,
      estimatedPaxCount = ar.bestPcpPaxEstimate(sourceOrderPreference),
      status = ar.displayStatus.description,
    )
  }

  case class TerminalFlightsJson(terminal: Terminal, flights: Iterable[FlightJson])

  case class PortFlightsJson(portCode: PortCode, terminals: Iterable[TerminalFlightsJson])

  def flights(flightsForDatesAndTerminals: (PortCode, List[FeedSource], LocalDate, LocalDate, Seq[Terminal]) => Source[ApiFlightWithSplits, NotUsed])
             (implicit ec: ExecutionContext, mat: Materializer): Seq[PortCode] => (SDateLike, SDateLike) => Future[FlightJsonResponse] =
    portCodes => (start, end) => {
      val dates = Set(start.toLocalDate, end.toLocalDate)

      Source(portCodes)
        .mapAsync(1) { portCode =>
          val eventualPortFlights = AirportConfigs.confByPort(portCode).terminals.map { terminal =>
            implicit val sourceOrder: List[FeedSource] = paxFeedSourceOrder(portCode)

            flightsForDatesAndTerminals(portCode, sourceOrder, dates.min, dates.max, Seq(terminal))
              .runWith(Sink.seq)
              .map { r =>
                val relevantFlights = r
                  .filter(_.apiFlight.hasPcpDuring(start, end, sourceOrder))
                  .map(f => FlightJson(f.apiFlight))

                TerminalFlightsJson(terminal, relevantFlights)
              }
          }

          Future
            .sequence(eventualPortFlights)
            .map(PortFlightsJson(portCode, _))
        }
        .runWith(Sink.seq)
        .map(FlightJsonResponse(start, end, _))
    }
}
