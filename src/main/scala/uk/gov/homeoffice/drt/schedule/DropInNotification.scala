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

sealed trait DropInNotificationCommand

object DropInNotification {

  private case object DropInNotificationCheck extends DropInNotificationCommand

  def apply(serverConfig: ServerConfig, timerInitialDelay: FiniteDuration, maxSize: Int, notifications: EmailNotifications): Behavior[DropInNotificationCommand] =
    Behaviors.setup { context: ActorContext[DropInNotificationCommand] =>
      implicit val ec: ExecutionContextExecutor = context.executionContext
      val dropInService: DropInService = new DropInService(DropInDao(ProdDatabase), DropInRegistrationDao(ProdDatabase), UserService(UserDao(ProdDatabase)),
        UserRequestService(UserAccessRequestDao(ProdDatabase)), serverConfig.teamEmail)

      Behaviors.withTimers(timers => new DropInNotification(
        notifications,
        dropInService,
        timers,
        timerInitialDelay,
        serverConfig.dropInNotificationFrequency.minutes,
        serverConfig.rootDomain,
        context).dropInNotificationReminder)
    }
}

class DropInNotification(notifications: EmailNotifications,
                         dropInService: DropInService,
                         timers: TimerScheduler[DropInNotificationCommand],
                         timerInitialDelay: FiniteDuration,
                         timerInterval: FiniteDuration,
                         rootDomain:String,
                         context: ActorContext[DropInNotificationCommand]) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  import DropInNotification._

  logger.info(s"Starting timer scheduler for Drop-In notification reminder $timerInterval")
  timers.startTimerWithFixedDelay(DropInNotificationCheck, timerInitialDelay, timerInterval)

  private def dropInNotificationReminder()(implicit ec: ExecutionContext): Behavior[DropInNotificationCommand] = {
    Behaviors.receiveMessage[DropInNotificationCommand] {
      case DropInNotificationCheck =>
        context.log.info("DropInNotificationCheck")
        dropInService.sendDropInNotificationToNewUsers(notifications,rootDomain)
        Behaviors.same

      case unknown =>
        logger.info(s"Unknown command: $unknown")
        Behaviors.same
    }
  }
}



