package uk.gov.homeoffice.drt.services

import akka.NotUsed
import akka.stream.scaladsl.Source
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
  val streamForGranularity: (LocalDate, LocalDate, Granularity, Option[Terminal]) => PortCode => Source[(Map[Queue, Int], Int, Int, Option[Any]), NotUsed] =
    (start, end, granularity, maybeTerminal) => portCode => {
      val queueTotals = PassengersHourlyDao.queueTotalsForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))
      val capacityTotals = CapacityHourlyDao.totalForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))
      val bxTotals = BorderCrossingDao.totalForPortAndDate(portCode.iata, maybeTerminal.map(_.toString))

      val queueTotalsQueryForDate: LocalDate => Future[Map[Queue, Int]] = date => db.run(queueTotals(date))
      val capacityTotalsForDate: LocalDate => Future[Int] = date => db.run(capacityTotals(date))
      val bxTotalsForDate: LocalDate => Future[Int] = date => db.run(bxTotals(date))

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


  private val hourlyStream: (LocalDate => Future[Map[Long, Map[Queue, Int]]], LocalDate => Future[Map[Long, Int]], LocalDate => Future[Map[Long, Int]]) => (LocalDate, LocalDate) => Source[(Map[Queue, Int], Int, Int, Option[Long]), NotUsed] =
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
          case (date, hourlyCaps, hourlyBx) =>
            queueHourlyForDate(date).map {
              _.toSeq.sortBy(_._1).map {
                case (hour, queueCounts) => (queueCounts, hourlyCaps.getOrElse(hour, 0), hourlyBx.getOrElse(hour, 0), Option(hour))
              }
            }
        }
        .mapConcat(identity)

  private val dailyStream: (LocalDate => Future[Map[Queue, Int]], LocalDate => Future[Int], LocalDate => Future[Int]) => (LocalDate, LocalDate) => Source[(Map[Queue, Int], Int, Int, Option[LocalDate]), NotUsed] =
    (queueTotalsForDate, capacityTotalForDate, bxTotalForDate) => (start, end) =>
      Source(DateRange(start, end))
        .mapAsync(1)(date => capacityTotalForDate(date).map(capacity => (date, capacity)))
        .mapAsync(1) { case (date, cap) =>
          bxTotalForDate(date).map(bx => (date, cap, bx))
        }
        .mapAsync(1) { case (date, cap, bx) =>
          queueTotalsForDate(date).map(queueCounts => (queueCounts, cap, bx, Option(date)))
        }

  private val totalsStream: (LocalDate => Future[Map[Queue, Int]], LocalDate => Future[Int], LocalDate => Future[Int]) => (LocalDate, LocalDate) => Source[(Map[Queue, Int], Int, Int, Option[LocalDate]), NotUsed] =
    (queueTotalsForDate, capacityTotalForDate, bxTotalForDate) => (start, end) =>
      Source(DateRange(start, end))
        .mapAsync(1)(date => capacityTotalForDate(date).map(capacity => (date, capacity)))
        .mapAsync(1) { case (date, cap) =>
          bxTotalForDate(date).map(bx => (date, cap, bx))
        }
        .mapAsync(1) { case (date, cap, bx) =>
          queueTotalsForDate(date).map(queues => (queues, cap, bx))
        }
        .fold((Map[Queue, Int](), 0, 0)) {
          case ((qAcc, capAcc, bxAcc), (queueCounts, capacity, bx)) =>
            val newQAcc = qAcc ++ queueCounts.map {
              case (queue, count) =>
                queue -> (qAcc.getOrElse(queue, 0) + count)
            }
            val newCapAcc = capAcc + capacity
            val newBxAcc = bxAcc + bx
            (newQAcc, newCapAcc, newBxAcc)
        }
        .map { case (queueCounts, cap, bx) =>
          (queueCounts, cap, bx, None)
        }
}
