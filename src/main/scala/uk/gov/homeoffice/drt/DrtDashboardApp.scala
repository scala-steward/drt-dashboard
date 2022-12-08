package uk.gov.homeoffice.drt

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.schedule.UserTracking
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
    userTrackingFeatureFlag = config.getBoolean("user-tracking.feature-flag"))

  val system: ActorSystem[Server.Message] = ActorSystem(Server(serverConfig), "DrtDashboard")
  if (serverConfig.userTrackingFeatureFlag) {
    ActorSystem(UserTracking(serverConfig, 1.minutes, 100), "UserTrackingTimer")
  }

}
