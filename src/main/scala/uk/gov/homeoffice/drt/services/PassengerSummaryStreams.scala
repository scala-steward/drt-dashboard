package uk.gov.homeoffice.drt.services

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import uk.gov.homeoffice.drt.db.AppDatabase
import uk.gov.homeoffice.drt.db.dao.{BorderCrossingDao, CapacityHourlyDao, PassengersHourlyDao}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.services.PassengerSummaryStreams.{Daily, Granularity, Hourly, Total}
import uk.gov.homeoffice.drt.time.{DateRange, LocalDate}

import scala.concurrent.{ExecutionContext, Future}

object PassengerSummaryStreams {
  sealed trait Granularity

  case object Hourly extends Granularity

  case object Daily extends Granularity

  case object Total extends Granularity

  object Granularity {
    def fromString(s: String): Granularity = s match {
      case "daily" => Daily
      case "total" => Total
      case _ => Hourly
    }
  }
}

case class PassengerSummaryStreams(db: AppDatabase)
                                  (implicit ec: ExecutionContext) {
  val streamForGranularity: (LocalDate, LocalDate, Granularity, Option[Terminal]) => PortCode => Source[(Map[Queue, Int], Int, Map[Queue, Int], Option[Any]), NotUsed] =
    (start, end, granularity, maybeTerminal) => portCode => {
      val drtQueueTotals = PassengersHourlyDao.queueTotalsForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))
      val capacityTotals = CapacityHourlyDao.totalForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))
      val bxQueueTotals = BorderCrossingDao.queueTotalsForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))

      val queueTotalsQueryForDate: LocalDate => Future[Map[Queue, Int]] = date => db.run(drtQueueTotals(date))
      val capacityTotalsForDate: LocalDate => Future[Int] = date => db.run(capacityTotals(date))
      val bxTotalsForDate: LocalDate => Future[Map[Queue, Int]] = date => db.run(bxQueueTotals(date))

      val stream = granularity match {
        case Hourly =>
          val hourlyQueueTotalsQuery = PassengersHourlyDao.hourlyForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))
          val hourlyCapacityTotalsQuery = CapacityHourlyDao.hourlyForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))
          val hourlyBxTotalsQuery = BorderCrossingDao.hourlyForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))
          val hourlyQueueTotalsForDate = (date: LocalDate) => db.run(hourlyQueueTotalsQuery(date))
          val hourlyCapacityTotalsForDate = (date: LocalDate) => db.run(hourlyCapacityTotalsQuery(date))
          val hourlyBxTotalsForDate = (date: LocalDate) => db.run(hourlyBxTotalsQuery(date))
          hourlyStream(hourlyQueueTotalsForDate, hourlyCapacityTotalsForDate, hourlyBxTotalsForDate)
        case Daily =>
          dailyStream(queueTotalsQueryForDate, capacityTotalsForDate, bxTotalsForDate)
        case Total =>
          totalsStream(queueTotalsQueryForDate, capacityTotalsForDate, bxTotalsForDate)
      }
      stream(start, end)
    }

  private val hourlyStream: (LocalDate => Future[Map[Long, Map[Queue, Int]]], LocalDate => Future[Map[Long, Int]], LocalDate => Future[Map[Long, Map[Queue, Int]]]) => (LocalDate, LocalDate) => Source[(Map[Queue, Int], Int, Map[Queue, Int], Option[Long]), NotUsed] =
    (queueHourlyForDate, capacityHourlyForDate, bxHourlyForDate) => (start, end) =>
      Source(DateRange(start, end))
        .mapAsync(1) { date =>
          capacityHourlyForDate(date).map { capacityTotals =>
            (date, capacityTotals)
          }
        }
        .mapAsync(1) { case (date, hourlyCaps) =>
          bxHourlyForDate(date).map { hourlyBx =>
            (date, hourlyCaps, hourlyBx)
          }
        }
        .mapAsync(1) {
          case (date, hourlyCaps, bxQueueCounts) =>
            queueHourlyForDate(date).map {
              _.toSeq.sortBy(_._1).map {
                case (hour, drtQueueCounts) =>
                  val bxQueueCountsForHour = bxQueueCounts.getOrElse(hour, Map.empty)
                  (drtQueueCounts, hourlyCaps.getOrElse(hour, 0), bxQueueCountsForHour, Option(hour))
              }
            }
        }
        .mapConcat(identity)

  private val dailyStream: (LocalDate => Future[Map[Queue, Int]], LocalDate => Future[Int], LocalDate => Future[Map[Queue, Int]]) => (LocalDate, LocalDate) => Source[(Map[Queue, Int], Int, Map[Queue, Int], Option[LocalDate]), NotUsed] =
    (queueTotalsForDate, capacityTotalForDate, bxTotalForDate) => (start, end) =>
      Source(DateRange(start, end))
        .mapAsync(1)(date => capacityTotalForDate(date).map(capacity => (date, capacity)))
        .mapAsync(1) { case (date, cap) =>
          bxTotalForDate(date).map(bx => (date, cap, bx))
        }
        .mapAsync(1) { case (date, cap, bx) =>
          queueTotalsForDate(date).map(queueCounts => (queueCounts, cap, bx, Option(date)))
        }

  private val totalsStream: (LocalDate => Future[Map[Queue, Int]], LocalDate => Future[Int], LocalDate => Future[Map[Queue, Int]]) => (LocalDate, LocalDate) => Source[(Map[Queue, Int], Int, Map[Queue, Int], Option[LocalDate]), NotUsed] =
    (queueTotalsForDate, capacityTotalForDate, bxTotalForDate) => (start, end) =>
      Source(DateRange(start, end))
        .mapAsync(1)(date => capacityTotalForDate(date).map(capacity => (date, capacity)))
        .mapAsync(1) { case (date, cap) =>
          bxTotalForDate(date).map(bx => (date, cap, bx))
        }
        .mapAsync(1) { case (date, cap, bx) =>
          queueTotalsForDate(date).map(queues => (queues, cap, bx))
        }
        .fold((Map.empty[Queue, Int], 0, Map.empty[Queue, Int])) {
          case ((qAcc, capAcc, bxAcc), (drtQueueCounts, capacity, bxQueueCounts)) =>
            val newQAcc: Map[Queue, Int] = addQueueCounts(qAcc, drtQueueCounts)
            val newCapAcc = capAcc + capacity
            val newBxAcc = addQueueCounts(bxAcc, bxQueueCounts)
            (newQAcc, newCapAcc, newBxAcc)
        }
        .map { case (queueCounts, cap, bx) =>
          (queueCounts, cap, bx, None)
        }

  private def addQueueCounts(qAcc: Map[Queue, Int], queueCounts: Map[Queue, Int]): Map[Queue, Int] =
    qAcc ++ queueCounts.map {
      case (queue, count) =>
        queue -> (qAcc.getOrElse(queue, 0) + count)
    }

}
