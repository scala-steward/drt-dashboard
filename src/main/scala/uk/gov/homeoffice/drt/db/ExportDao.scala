package uk.gov.homeoffice.drt.db

import slick.lifted.Tag
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}

import java.sql.Timestamp
import scala.concurrent.ExecutionContext

class PassengersHourlyTable(tag: Tag)
  extends Table[(String, Int, String, String, Int, Timestamp, Timestamp)](tag, "passengers_hourly") {

  def dateUtc: Rep[String] = column[String]("date_utc")

  def hour: Rep[Int] = column[Int]("hour")

  def port: Rep[String] = column[String]("port")

  def terminal: Rep[String] = column[String]("terminal")

  def passengers: Rep[Int] = column[Int]("passengers")

  def createdAt: Rep[Timestamp] = column[java.sql.Timestamp]("created_at")

  def updatedAt: Rep[Timestamp] = column[java.sql.Timestamp]("updated_at")

  def pk = primaryKey("pk_date_utc_hour_port_terminal", (dateUtc, hour, port, terminal))

  def * = (dateUtc, hour, port, terminal, passengers, createdAt, updatedAt)
}

object PassengersHourlyQueries {
  val table: TableQuery[PassengersHourlyTable] = TableQuery[PassengersHourlyTable]

  def getTotalForPortAndDate(port: String)
                            (implicit ec: ExecutionContext):
  LocalDate => DBIOAction[Int, NoStream, Effect.Read] =
    localDate => {
      val sdate = SDate(localDate)
      val utcDates = Set(
        sdate.getLocalLastMidnight.toUtcDate,
        sdate.getLocalNextMidnight.toUtcDate,
      )

      table
        .filter(_.port === port.toLowerCase)
        .filter(_.dateUtc inSet utcDates.map(_.toISOString))
        .result
        .map { rows =>
          rows.filter {
            case (utc, hour, _, _, pax, _, _) =>
              val Array(y, m, d) = utc.split("-").map(_.toInt)
              val rowLocalDate = SDate(UtcDate(y, m, d)).addHours(hour).toLocalDate
              rowLocalDate == localDate
          }.map(_._5).sum
        }
    }
}
