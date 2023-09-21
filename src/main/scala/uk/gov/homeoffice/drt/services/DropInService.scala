package uk.gov.homeoffice.drt.services

import org.joda.time.DateTime
import uk.gov.homeoffice.drt.ServerConfig
import uk.gov.homeoffice.drt.db.{DropInDao, DropInRegistrationDao, DropInRegistrationRow}
import uk.gov.homeoffice.drt.notifications.EmailNotifications

import scala.concurrent.{ExecutionContext, Future}

class DropInService(dropInDao: DropInDao, dropInRegistrationDao: DropInRegistrationDao, serverConfig: ServerConfig) {

  def sendSeminarReminders(notifications: EmailNotifications)(implicit ec: ExecutionContext): Future[Unit] = {
    val notifyDate: Long = DateTime.now().withTimeAtStartOfDay.plusDays(7).getMillis
    val presentDate: Long = DateTime.now().withTimeAtStartOfDay().minusDays(1).getMillis
    dropInDao.getDropInDueForNotifying(notifyDate, presentDate).map { dropInsToNotify =>
      dropInsToNotify.foreach { dropIn =>
        dropIn.id
          .map(id => dropInRegistrationDao.getRegisteredUsersToNotify(id.toString, dropIn.startTime))
          .getOrElse(Future.successful(Seq.empty[DropInRegistrationRow]))
          .map { usersToNotify =>
            usersToNotify.map(user => notifications.sendDropInReminderEmail(user.email, dropIn, serverConfig.teamEmail))
            usersToNotify.map(user => dropInRegistrationDao.updateEmailSentTime(user.dropInId.toString))
          }
      }
    }
  }

}
