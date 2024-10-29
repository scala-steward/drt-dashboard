package uk.gov.homeoffice.drt.schedule

import akka.actor
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior, Scheduler}
import akka.util.Timeout
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.ServerConfig
import uk.gov.homeoffice.drt.db.{ProdDatabase, UserDao, UserRow}
import uk.gov.homeoffice.drt.keycloak.KeyCloakAuthTokenService.GetToken
import uk.gov.homeoffice.drt.keycloak.{KeyCloakAuthToken, KeyCloakAuthTokenService, KeyCloakUser, KeycloakService}
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.notifications.templates.AccessRequestTemplates.{inactiveUserNotificationTemplateId, revokeAccessTemplateId}
import uk.gov.homeoffice.drt.services.UserService

import java.sql.Timestamp
import java.util.Date
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

sealed trait Command

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object UserTracking {
  private case object UserTrackingKey extends Command

  private case object UserTrackingRevokeKey extends Command

  private case object InactiveUserCheck extends Command

  private case object RevokeUserCheck extends Command

  private case class PerformAccountRevocations(token: KeyCloakAuthToken) extends Command

  def apply(serverConfig: ServerConfig, timerInitialDelay: FiniteDuration, maxSize: Int, notifications: EmailNotifications): Behavior[Command] =
    Behaviors.setup { context: ActorContext[Command] =>
      implicit val ec: ExecutionContextExecutor = context.executionContext
      val userService: UserService = UserService(UserDao(ProdDatabase.db))

      Behaviors.withTimers(timers => new UserTracking(
        serverConfig,
        notifications,
        userService,
        timers, timerInitialDelay,
        serverConfig.dormantUsersCheckFrequency.minutes,
        serverConfig.inactivityDays,
        serverConfig.deactivateAfterWarningDays,
        maxSize, context).userBehaviour)
    }
}

class UserTracking(serverConfig: ServerConfig,
  notifications: EmailNotifications,
  userService: UserService,
  timers: TimerScheduler[Command],
  timerInitialDelay: FiniteDuration,
  timerInterval: FiniteDuration,
  numberOfInactivityDays: Int,
  deactivateAfterWarningDays: Int,
  maxSize: Int,
  context: ActorContext[Command]) {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  import UserTracking._

  logger.info(s"Starting timer scheduler for user tracking $timerInterval")
  timers.startTimerWithFixedDelay(UserTrackingKey, InactiveUserCheck, timerInitialDelay, timerInterval)
  timers.startTimerWithFixedDelay(UserTrackingRevokeKey, RevokeUserCheck, timerInitialDelay, timerInterval)
  private val keyCloakAuthTokenService: Behavior[KeyCloakAuthTokenService.Token] =
    KeyCloakAuthTokenService.getTokenBehavior(serverConfig.keyClockConfig, serverConfig.keycloakUsername, serverConfig.keycloakPassword)
  private val keycloakServiceBehavior: ActorRef[KeyCloakAuthTokenService.Token] = context.spawn(keyCloakAuthTokenService, "keycloakServiceActor")

  private def userBehaviour()(implicit ec: ExecutionContext): Behavior[Command] = {
    Behaviors.receiveMessage[Command] {
      case InactiveUserCheck =>
        context.log.info("InactiveUserCheck")
        val users = userService.getInactiveUsers(numberOfInactivityDays)
        users.map(
          _.map { user =>
            if (user.email.nonEmpty) {
              notifications.sendUserInactivityEmailNotification(
                user.email,
                serverConfig.rootDomain,
                serverConfig.teamEmail,
                inactiveUserNotificationTemplateId,
                "inactive user notification")
              logger.info(s"User with email ${user.email} notified due to inactivity")
            } else {
              logger.info(s"No email for $user to notify")
            }
            userService.upsertUser(user.copy(inactive_email_sent = Some(new Timestamp(new Date().getTime))), Some("inactivity"))
          })
        Behaviors.same

      case RevokeUserCheck =>
        implicit val timeout: Timeout = new Timeout(30.seconds)
        implicit val scheduler: Scheduler = context.system.scheduler
        keycloakServiceBehavior.ask(ref => GetToken(ref)).map(token => context.self ! PerformAccountRevocations(token))
        Behaviors.same

      case PerformAccountRevocations(token: KeyCloakAuthToken) =>
        context.log.info("KeyCloakToken-RevokeAccess")
        implicit val actorSystem: actor.ActorSystem = context.system.classicSystem
        val usersToRevoke = userService.getUsersToRevoke(numberOfInactivityDays, deactivateAfterWarningDays).map(_.take(maxSize))
        val keyClockClient = KeyCloakAuthTokenService.getKeyClockClient(serverConfig.keyClockConfig.url, token)
        val keycloakService = KeycloakService(keyClockClient)
        usersToRevoke.map { utrOption =>
          utrOption.map { userToRevoke: UserRow =>
            if (userToRevoke.email.nonEmpty) {
              keycloakService.getUserForEmail(userToRevoke.email).map { ud =>
                ud.map { userFromKeycloak =>
                  if (userToRevoke.email.toLowerCase.trim == userFromKeycloak.email.toLowerCase.trim) {
                    removeUser(keycloakService, userFromKeycloak, userToRevoke)
                    notifications.sendUserInactivityEmailNotification(
                      userFromKeycloak.email,
                      serverConfig.rootDomain,
                      serverConfig.teamEmail,
                      revokeAccessTemplateId,
                      "revoked DRT Access")
                    logger.info(s"User with email ${userToRevoke.email} access revoked due to inactivity")
                  }
                }
              }
            } else {
              keycloakService.getUserForUsername(userToRevoke.username).map { ud =>
                ud.map { userFromKeycloak =>
                  if (userToRevoke.username.toLowerCase.trim == userFromKeycloak.username.toLowerCase.trim) {
                    removeUser(keycloakService, userFromKeycloak, userToRevoke)
                    logger.info(s"User with username ${userToRevoke.username} access revoked due to inactivity")
                  }
                }
              }
            }

          }
        }
        Behaviors.same

      case _ =>
        logger.info(s"Unknown command to log")
        Behaviors.same
    }
  }

  def removeUser(keycloakService: KeycloakService, uId: KeyCloakUser, utr: UserRow)
    (implicit ec: ExecutionContext): Future[Int] = {
    keycloakService.removeUser(uId.id)
    userService.upsertUser(utr.copy(revoked_access = Some(new Timestamp(new Date().getTime))), Some("revoked"))
  }
}

