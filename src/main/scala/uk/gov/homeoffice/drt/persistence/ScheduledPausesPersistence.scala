package uk.gov.homeoffice.drt.persistence

import uk.gov.homeoffice.drt.db.{AppDatabase, ScheduledHealthCheckPauseDao}
import uk.gov.homeoffice.drt.healthchecks.ScheduledPause
import uk.gov.homeoffice.drt.time.SDateLike

import scala.concurrent.{ExecutionContext, Future}

trait ScheduledHealthCheckPausePersistence {
  def insert(export: ScheduledPause): Future[Int]

  def get(maybeAfter: Option[Long]): Future[Seq[ScheduledPause]]

  def delete(from: Long, to: Long): Future[Int]
}

case class ScheduledHealthCheckPausePersistenceImpl(database: AppDatabase, now: () => SDateLike)
                                                   (implicit ec: ExecutionContext) extends ScheduledHealthCheckPausePersistence {
  private val dao: ScheduledHealthCheckPauseDao = ScheduledHealthCheckPauseDao(now)

  override def insert(export: ScheduledPause): Future[Int] = database.db.run(dao.insert(export))

  override def get(maybeAfter: Option[Long]): Future[Seq[ScheduledPause]] = database.db.run(dao.get(maybeAfter))

  override def delete(from: Long, to: Long): Future[Int] = database.db.run(dao.delete(from, to))
}
