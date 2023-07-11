package uk.gov.homeoffice.drt

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.schedule.UserTracking
import uk.gov.homeoffice.drt.services.s3.{ProdS3MultipartUploader, S3Downloader, S3Uploader}
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
    keyclockUrl = config.getString("key-cloak.url"),
    keyclockTokenUrl = config.getString("key-cloak.token_url"),
    keyclockClientId = config.getString("key-cloak.client_id"),
    keyclockClientSecret = config.getString("key-cloak.client_secret"),
    keyclockUsername = config.getString("key-cloak.username"),
    keyclockPassword = config.getString("key-cloak.password"),
    scheduleFrequency = config.getInt("user-tracking.schedule-frequency-minutes"),
    inactivityDays = config.getInt("user-tracking.inactivity-days"),
    userTrackingFeatureFlag = config.getBoolean("user-tracking.feature-flag"),
    deactivateAfterWarningDays= config.getInt("user-tracking.deactivate-after-warning-days")
  )

  val accessKey = config.getString("s3.credentials.access_key_id")
  val secretKey = config.getString("s3.credentials.secret_key")
  val bucketName = config.getString("s3.bucket-name")
  val folderPrefix = config.getString("exports.s3-folder-prefix")

  val credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))

  val s3Client: S3AsyncClient = S3AsyncClient.builder()
    .region(Region.EU_WEST_2)
    .credentialsProvider(credentialsProvider)
    .build()

  val multipartUploader = ProdS3MultipartUploader(s3Client)
  val uploader = S3Uploader(multipartUploader, bucketName, Option(folderPrefix))
  val downloader = S3Downloader(s3Client, bucketName)

  val emailNotifications = EmailNotifications(serverConfig.accessRequestEmails, new NotificationClient(serverConfig.notifyServiceApiKey))

  val system: ActorSystem[Server.Message] = ActorSystem(Server(serverConfig, emailNotifications, uploader, downloader), "DrtDashboard")
  if (serverConfig.userTrackingFeatureFlag) {
    ActorSystem(UserTracking(serverConfig, 1.minutes, 100, emailNotifications), "UserTrackingTimer")
  }

}
