package uk.gov.homeoffice.drt.db

import org.joda.time.DateTime
import slick.dbio.Effect
import slick.lifted.{ProvenShape, Tag}
import slick.sql.FixedSqlAction
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.healthchecks.ScheduledPause
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.time.SDateLike

import java.sql.Timestamp
import scala.concurrent.ExecutionContext

case class ScheduledHealthCheckPauseRow(startsAt: Timestamp, endsAt: Timestamp, ports: String, createdAt: Timestamp)

class ScheduledHealthCheckPauseTable(tag: Tag) extends Table[ScheduledHealthCheckPauseRow](tag, "scheduled_health_check_pause") {
  def startsAt: Rep[Timestamp] = column[Timestamp]("starts_at")

  def endsAt: Rep[Timestamp] = column[Timestamp]("ends_at")

  def ports: Rep[String] = column[String]("ports")

  def createdAt: Rep[Timestamp] = column[Timestamp]("created_at")

  def * : ProvenShape[ScheduledHealthCheckPauseRow] = (startsAt, endsAt, ports, createdAt).mapTo[ScheduledHealthCheckPauseRow]

  val pk = primaryKey("scheduled_health_check_pause_pkey", (startsAt, endsAt))
}


case class ScheduledHealthCheckPauseDao(now: () => SDateLike) {
  val table: TableQuery[ScheduledHealthCheckPauseTable] = TableQuery[ScheduledHealthCheckPauseTable]

  def insert(pause: ScheduledPause): DBIOAction[Int, NoStream, Effect.Write] = {
    table += ScheduledHealthCheckPauseRow(
      new Timestamp(pause.startsAt.getMillis),
      new Timestamp(pause.endsAt.getMillis),
      pause.ports.map(_.iata).mkString(","),
      new Timestamp(now().millisSinceEpoch)
    )
  }

  def get(maybeAfter: Option[Long])
         (implicit ec: ExecutionContext): DBIOAction[Seq[ScheduledPause], NoStream, Effect.Read] = {
    val query = table.sortBy(_.startsAt)
    maybeAfter
      .map(after => query.filter(_.startsAt > new Timestamp(after)))
      .getOrElse(query)
      .result
      .map(_.map(r =>
        ScheduledPause(
          new DateTime(r.startsAt.getTime),
          new DateTime(r.endsAt.getTime),
          r.ports.split(",").map(PortCode(_)),
          new DateTime(r.createdAt.getTime))
      ))
  }

  def delete(startsAt: Long, endsAt: Long): FixedSqlAction[Int, NoStream, Effect.Write] = {
    table
      .filter(r => r.startsAt === new Timestamp(startsAt) && r.endsAt === new Timestamp(endsAt))
      .delete
  }
}
