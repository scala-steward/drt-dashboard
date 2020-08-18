package uk.gov.homeoffice.drt

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory

object DrtDashboardApp extends App {
  val config = ConfigFactory.load()

  val serverHost = config.getString("server.host")
  val serverPort = config.getInt("server.port")
  val portCodes = config.getString("dashboard.port-codes").split(",")
  val ciriumDataUri = config.getString("cirium.data-uri")

  val system: ActorSystem[Server.Message] = ActorSystem(Server(serverHost, serverPort, portCodes, ciriumDataUri), "DrtDashboard")
}
