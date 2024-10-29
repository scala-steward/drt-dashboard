package uk.gov.homeoffice.drt.services

import org.joda.time.DateTime
import uk.gov.homeoffice.drt.db.{DropInDao, DropInRegistrationDao, DropInRegistrationRow}
import uk.gov.homeoffice.drt.notifications.EmailNotifications

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}

class DropInService(dropInDao: DropInDao, dropInRegistrationDao: DropInRegistrationDao, userService: UserService, userRequestService: UserRequestService, teamEmail: String) {

  def sendSeminarReminders(notifications: EmailNotifications)(implicit ec: ExecutionContext): Future[Unit] = {
    val notifyDate: Long = DateTime.now().withTimeAtStartOfDay.plusDays(7).getMillis
    val presentDate: Long = DateTime.now().withTimeAtStartOfDay().minusDays(1).getMillis
    dropInDao.getDropInDueForNotifying(notifyDate, presentDate).map { dropInsToNotify =>
      dropInsToNotify.foreach { dropIn =>
        dropIn.id
          .map(id => dropInRegistrationDao.getRegisteredUsersToNotify(id.toString, dropIn.startTime))
          .getOrElse(Future.successful(Seq.empty[DropInRegistrationRow]))
          .map { usersToNotify =>
            usersToNotify.map(user => notifications.sendDropInReminderEmail(user.email, dropIn, teamEmail))
            usersToNotify.map(user => dropInRegistrationDao.updateEmailSentTime(user.dropInId.toString))
          }
      }
    }
  }


  def sendDropInNotificationToNewUsers(notifications: EmailNotifications, rootDomain: String)(implicit ec: ExecutionContext) = {
    userService.getUsersWithoutDropInNotification.flatMap { users =>
      Future.sequence(users.map { user =>
        dropInRegistrationDao.findRegistrationsByEmail(user.email).flatMap {
          case list if list.isEmpty =>
            userRequestService.getUserRequestByEmail(user.email).map(_.headOption).map { userAccessRequest =>
              notifications.sendDropInNotification(userAccessRequest, rootDomain, teamEmail)
              userService.upsertUser(user.copy(drop_in_notification_at = Option(new Timestamp(new DateTime().getMillis))), Some("dropInNotification"))
            }

          case _ =>
            Future.successful(0)
        }
      })
    }
  }

}
