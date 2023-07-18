package uk.gov.homeoffice.drt

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.schedule.UserTracking
import uk.gov.service.notify.NotificationClient

import scala.concurrent.duration.DurationInt

object DrtDashboardApp extends App {
  val config = ConfigFactory.load()

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
    neboPortCodes = config.getString("nebo.port-codes").split(","),
    keycloakUrl = config.getString("key-cloak.url"),
    keycloakTokenUrl = config.getString("key-cloak.token_url"),
    keycloakClientId = config.getString("key-cloak.client_id"),
    keycloakClientSecret = config.getString("key-cloak.client_secret"),
    keycloakUsername = config.getString("key-cloak.username"),
    keycloakPassword = config.getString("key-cloak.password"),
    scheduleFrequency = config.getInt("user-tracking.schedule-frequency-minutes"),
    inactivityDays = config.getInt("user-tracking.inactivity-days"),
    userTrackingFeatureFlag = config.getBoolean("user-tracking.feature-flag"),
    deactivateAfterWarningDays = config.getInt("user-tracking.deactivate-after-warning-days"),
    s3AccessKey = config.getString("s3.credentials.access_key_id"),
    s3SecretAccessKey = config.getString("s3.credentials.secret_key"),
    drtS3BucketName = config.getString("s3.bucket-name"),
    exportsFolderPrefix = config.getString("exports.s3-folder-prefix"),
    featureFolderPrefix = config.getString("feature-guides.s3-folder-prefix")
  )


  val emailNotifications = EmailNotifications(serverConfig.accessRequestEmails, new NotificationClient(serverConfig.notifyServiceApiKey))

  val system: ActorSystem[Server.Message] = ActorSystem(Server(serverConfig, emailNotifications), "DrtDashboard")
  if (serverConfig.userTrackingFeatureFlag) {
    ActorSystem(UserTracking(serverConfig, 1.minutes, 100, emailNotifications), "UserTrackingTimer")
  }

}
