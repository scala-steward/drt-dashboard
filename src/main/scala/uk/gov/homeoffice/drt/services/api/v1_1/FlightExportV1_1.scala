package uk.gov.homeoffice.drt.services.api.v1_1

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import uk.gov.homeoffice.drt.Server.paxFeedSourceOrder
import uk.gov.homeoffice.drt.arrivals.ApiFlightWithSplits
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.routes.api.v1_1.FlightApiV1_1Routes.{FlightJsonV1_1, FlightJsonResponseV1_1}
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

import scala.concurrent.{ExecutionContext, Future}

object FlightExportV1_1 {
  def flights(flightsForDatesAndTerminals: (PortCode, List[FeedSource], LocalDate, LocalDate, Seq[Terminal]) => Source[ApiFlightWithSplits, NotUsed])
             (implicit ec: ExecutionContext, mat: Materializer): Seq[PortCode] => (SDateLike, SDateLike) => Future[FlightJsonResponseV1_1] =
    portCodes => (start, end) => {
      val startLocal = start.toLocalDate
      val endLocal = end.toLocalDate

      val dates = Set(startLocal, endLocal)

      Source(portCodes)
        .mapAsync(1) { portCode =>
          val terminals = AirportConfigs.confByPort(portCode).terminalsForDateRange(startLocal, endLocal)
          val eventualPortFlights = terminals.map { terminal =>
            implicit val sourceOrder: List[FeedSource] = paxFeedSourceOrder(portCode)

            flightsForDatesAndTerminals(portCode, sourceOrder, dates.min, dates.max, Seq(terminal))
              .runWith(Sink.seq)
              .map {
                _
                  .filter(_.apiFlight.hasPcpDuring(start, end, sourceOrder))
                  .map(f => FlightJsonV1_1(portCode, f))
              }
          }

          Future
            .sequence(eventualPortFlights)
            .map(_.flatten)
        }
        .runWith(Sink.fold(Seq.empty[FlightJsonV1_1])(_ ++ _))
        .map(FlightJsonResponseV1_1(start, end, _))
    }
}
