package uk.gov.homeoffice.drt

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import uk.gov.homeoffice.drt.Server.ServerConfig

object DrtDashboardApp extends App {
  val config = ConfigFactory.load()

  val serverConfig = ServerConfig(
    host = config.getString("server.host"),
    port = config.getInt("server.port"),
    portCodes = config.getString("dashboard.port-codes").split(","),
    ciriumDataUri = config.getString("cirium.data-uri"),
    drtDomain = config.getString("drt.domain"),
    accessRequestEmail = config.getString("dashboard.notifications.access-request-email"),
    notifyServiceApiKey = config.getString("dashboard.notifications.gov-notify-api-key"))

  val system: ActorSystem[Server.Message] = ActorSystem(Server(serverConfig), "DrtDashboard")
}
