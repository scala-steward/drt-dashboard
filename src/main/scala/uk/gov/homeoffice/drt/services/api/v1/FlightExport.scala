package uk.gov.homeoffice.drt.services.api.v1

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import uk.gov.homeoffice.drt.Server.paxFeedSourceOrder
import uk.gov.homeoffice.drt.arrivals.ApiFlightWithSplits
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.routes.api.v1.FlightApiV1Routes.{FlightJson, FlightJsonResponse}
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

import scala.concurrent.{ExecutionContext, Future}

object FlightExport {
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
              .map {
                _
                  .filter(_.apiFlight.hasPcpDuring(start, end, sourceOrder))
                  .map(f => FlightJson(portCode, f.apiFlight))
              }
          }

          Future
            .sequence(eventualPortFlights)
            .map(_.flatten)
        }
        .runWith(Sink.fold(Seq.empty[FlightJson])(_ ++ _))
        .map(FlightJsonResponse(start, end, _))
    }
}
