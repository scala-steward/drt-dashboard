package uk.gov.homeoffice.drt.persistence
import uk.gov.homeoffice.drt.healthchecks.ScheduledPause

import scala.concurrent.Future

object MockScheduledHealthCheckPausePersistence extends ScheduledHealthCheckPausePersistence {
  var pauses: Seq[ScheduledPause] = Seq[ScheduledPause]()
  override def insert(export: ScheduledPause): Future[Int] = {
    pauses = pauses :+ export
    Future.successful(1)
  }

  override def get(maybeAfter: Option[Long]): Future[Seq[ScheduledPause]] = Future.successful(pauses)

  override def delete(from: Long, to: Long): Future[Int] = {
    pauses = pauses.filter(p => p.startsAt.getMillis != from && p.endsAt.getMillis != to)
    Future.successful(1)
  }
}
