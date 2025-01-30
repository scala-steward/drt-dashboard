package uk.gov.homeoffice.drt.schedule

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.ServerConfig
import uk.gov.homeoffice.drt.db.{DropInDao, DropInRegistrationDao, ProdDatabase, UserAccessRequestDao, UserDao}
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.services.{DropInService, UserRequestService, UserService}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

sealed trait DropInCommand

object DropInReminder {
  private case object UserDropInNotifyKey extends DropInCommand

  private case object DropInUserNotificationCheck extends DropInCommand

  def apply(serverConfig: ServerConfig, timerInitialDelay: FiniteDuration, maxSize: Int, notifications: EmailNotifications): Behavior[DropInCommand] =
    Behaviors.setup { context: ActorContext[DropInCommand] =>
      implicit val ec: ExecutionContextExecutor = context.executionContext
      val dropInService: DropInService = new DropInService(DropInDao(ProdDatabase),
        DropInRegistrationDao(ProdDatabase),
        UserService(UserDao(ProdDatabase)),
        UserRequestService(UserAccessRequestDao(ProdDatabase)), serverConfig.teamEmail)

      Behaviors.withTimers(timers => new DropInReminder(
        notifications,
        dropInService,
        timers,
        timerInitialDelay,
        serverConfig.dropInRemindersCheckFrequency.minutes,
        context).dropInReminderNotification)
    }
}

class DropInReminder(notifications: EmailNotifications,
                     dropInService: DropInService,
                     timers: TimerScheduler[DropInCommand],
                     timerInitialDelay: FiniteDuration,
                     timerInterval: FiniteDuration,
                     context: ActorContext[DropInCommand]) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  import DropInReminder._

  logger.info(s"Starting timer scheduler for Drop-In reminder $timerInterval")
  timers.startTimerWithFixedDelay(UserDropInNotifyKey, DropInUserNotificationCheck, timerInitialDelay, timerInterval)

  private def dropInReminderNotification()(implicit ec: ExecutionContext): Behavior[DropInCommand] = {
    Behaviors.receiveMessage[DropInCommand] {
      case DropInUserNotificationCheck =>
        context.log.info("SeminarUserNotificationCheck")
        dropInService.sendSeminarReminders(notifications)
        Behaviors.same

      case unknown =>
        logger.info(s"Unknown command: $unknown")
        Behaviors.same
    }
  }
}



