package uk.gov.homeoffice.drt.healthchecks

import uk.gov.homeoffice.drt.persistence.ScheduledHealthCheckPausePersistence
import uk.gov.homeoffice.drt.time.SDate

import scala.concurrent.{ExecutionContext, Future}

object CheckScheduledPauses {
  def pausesProvider(pausePersistence: ScheduledHealthCheckPausePersistence): () => Future[Seq[ScheduledPause]] =
    () => pausePersistence.get(Some(SDate.now().millisSinceEpoch))

  def activePauseChecker(pauses: () => Future[Seq[ScheduledPause]])
                        (implicit ec: ExecutionContext): () => Future[Boolean] =
    () => pauses().map(_.exists(p =>
      p.startsAt.getMillis <= SDate.now().millisSinceEpoch && SDate.now().millisSinceEpoch <= p.endsAt.getMillis))
}
