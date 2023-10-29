package uk.gov.homeoffice.drt

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import uk.gov.homeoffice.drt.notifications.{EmailClientImpl, EmailNotifications}
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.schedule.{DropInReminder, UserTracking}
import uk.gov.service.notify.NotificationClient

import scala.concurrent.duration.DurationInt

object DrtDashboardApp extends App {
  val config = ConfigFactory.load()

  private val ports = config.getString("enabled-ports") match {
    case "" => PortRegion.regions.flatMap(_.ports).toSeq
    case portList => portList.split(",").map(PortCode(_)).toSeq
  }

  val serverConfig = ServerConfig(
    host = config.getString("server.host"),
    port = config.getInt("server.port"),
    teamEmail = config.getString("dashboard.team-email"),
    portRegions = PortRegion.regions,
    ciriumDataUri = config.getString("cirium.data-uri"),
    rootDomain = config.getString("drt.domain"),
    useHttps = config.getBoolean("drt.use-https"),
    accessRequestEmails = config.getString("dashboard.notifications.access-request-emails").split(",").toList,
    notifyServiceApiKey = config.getString("dashboard.notifications.gov-notify-api-key"),
    keycloakUrl = config.getString("key-cloak.url"),
    keycloakTokenUrl = config.getString("key-cloak.token_url"),
    keycloakClientId = config.getString("key-cloak.client_id"),
    keycloakClientSecret = config.getString("key-cloak.client_secret"),
    keycloakUsername = config.getString("key-cloak.username"),
    keycloakPassword = config.getString("key-cloak.password"),
    dormantUsersCheckFrequency = config.getInt("user-tracking.schedule-frequency-minutes"),
    dropInRemindersCheckFrequency = config.getInt("drop-in-registration.schedule-frequency-minutes"),
    inactivityDays = config.getInt("user-tracking.inactivity-days"),
    userTrackingFeatureFlag = config.getBoolean("user-tracking.feature-flag"),
    deactivateAfterWarningDays = config.getInt("user-tracking.deactivate-after-warning-days"),
    s3AccessKey = config.getString("s3.credentials.access_key_id"),
    s3SecretAccessKey = config.getString("s3.credentials.secret_key"),
    drtS3BucketName = config.getString("s3.bucket-name"),
    exportsFolderPrefix = config.getString("exports.s3-folder-prefix"),
    featureFolderPrefix = config.getString("feature-guides.s3-folder-prefix"),
    portCodes = ports,
    healthCheckTriggeredNotifyTemplateId = config.getString("health-checks.notify-templates.triggered"),
    healthCheckResolvedNotifyTemplateId = config.getString("health-checks.notify-templates.resolved"),
    healthCheckEmailRecipient = config.getString("health-checks.email-recipient"),
    healthCheckFrequencyMinutes = config.getInt("health-checks.frequency-minutes"),
  )

  private val govNotifyClient = new NotificationClient(serverConfig.notifyServiceApiKey)

  val emailClient: EmailClientImpl = EmailClientImpl(govNotifyClient)

  private val emailNotifications = EmailNotifications(serverConfig.accessRequestEmails, govNotifyClient)

  val system: ActorSystem[Server.Message] = ActorSystem(Server(serverConfig, emailNotifications, emailClient), "DrtDashboard")
  if (serverConfig.userTrackingFeatureFlag) {
    ActorSystem(UserTracking(serverConfig, 1.minutes, 100, emailNotifications), "UserTrackingTimer")
    ActorSystem(DropInReminder(serverConfig, 1.minutes, 100, emailNotifications), "SeminarReminderTimer")
  }

}
