package uk.gov.homeoffice.drt.services.api.v1_1

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.ArrivalGenerator
import uk.gov.homeoffice.drt.arrivals.EventTypes.DC
import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, Splits}
import uk.gov.homeoffice.drt.ports.PaxTypes.EeaMachineReadable
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk}
import uk.gov.homeoffice.drt.ports.SplitRatiosNs.SplitSources.ApiSplitsWithHistoricalEGateAndFTPercentages
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{ApiPaxTypeAndQueueCount, FeedSource, LiveFeedSource, PortCode}
import uk.gov.homeoffice.drt.routes.api.v1_1.FlightApiV1_1Routes.{FlightJsonResponseV1_1, FlightJsonV1_1, FlightQueuePaxCountJsonV1_1}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}


class FlightExportTest extends AnyWordSpec with Matchers {
  implicit val system: ActorSystem = ActorSystem("FlightExportSpec")
  implicit val mat: Materializer = Materializer.matFromSystem
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  implicit val sourceOrderPreference: List[FeedSource] = List(LiveFeedSource)

  val startMinute: SDateLike = SDate("2024-10-15T12:00")
  val endMinute: SDateLike = SDate("2024-10-15T14:00")

  val egatePct = 0.75
  val eeaPct = 0.25

  def splits(totalPax: Int): Splits = {
    Splits(
      Set(
        ApiPaxTypeAndQueueCount(EeaMachineReadable, EeaDesk, eeaPct * totalPax, None, None),
        ApiPaxTypeAndQueueCount(EeaMachineReadable, EGate, egatePct * totalPax, None, None)),
      ApiSplitsWithHistoricalEGateAndFTPercentages, Some(DC))
  }


  "FlightExport" should {
    "return a PortFlightsJson with the correct structure and only the flight with passengers in the requested time range" in {
      val sched1 = SDate("2024-10-15T12:00")
      val sched2 = SDate("2024-10-15T13:55")
      val source = (_: PortCode, _: List[FeedSource], _: LocalDate, _: LocalDate, _: Seq[Terminal]) => {
        Source(
          Seq(
            ArrivalGenerator.arrival(iata = "BA0001", schDt = "2024-10-15T11:00", totalPax = Option(100), transPax = Option(10), feedSource = LiveFeedSource),
            ArrivalGenerator.arrival(iata = "BA0002", schDt = sched1.toISOString, estDt = sched1.addMinutes(1).toISOString,
              actChoxDt = sched1.addMinutes(5).toISOString, totalPax = Option(100), transPax = Option(10), feedSource = LiveFeedSource),
            ArrivalGenerator.arrival(iata = "BA0003", schDt = sched2.toISOString, totalPax = Option(200), transPax = Option(10), feedSource = LiveFeedSource),
            ArrivalGenerator.arrival(iata = "BA0004", schDt = "2024-10-15T15:00", totalPax = Option(200), transPax = Option(10), feedSource = LiveFeedSource),
          ).map(a => ApiFlightWithSplits(a, Set(splits(a.bestPcpPaxEstimate(sourceOrderPreference).getOrElse(0)))))
        )
      }
      val export = FlightExportV1_1.flights(source)
      Await.result(export(Seq(PortCode("STN")))(startMinute, endMinute), 1.second) should ===(
        FlightJsonResponseV1_1(
          startMinute,
          endMinute,
          Seq(
            FlightJsonV1_1("STN", "T1", "BA0002", "JFK", "John F Kennedy Intl", sched1.millisSinceEpoch,
              Option(sched1.addMinutes(1).millisSinceEpoch), Option(sched1.addMinutes(5).millisSinceEpoch),
              Some(sched1.addMinutes(5).millisSinceEpoch), Some(sched1.addMinutes(9).millisSinceEpoch), Some(90), "On Chocks", Some(Seq(FlightQueuePaxCountJsonV1_1("e-Gates", 67), FlightQueuePaxCountJsonV1_1("EEA", 23)))),
            FlightJsonV1_1("STN", "T1", "BA0003", "JFK", "John F Kennedy Intl", sched2.millisSinceEpoch,
              None, None,
              Some(sched2.addMinutes(5).millisSinceEpoch), Some(sched2.addMinutes(14).millisSinceEpoch), Some(190), "Scheduled", Some(Seq(FlightQueuePaxCountJsonV1_1("e-Gates", 142), FlightQueuePaxCountJsonV1_1("EEA", 48)))),
          )
        )
      )
    }
  }
}
