package uk.gov.homeoffice.drt.routes

import com.typesafe.config.{ Config, ConfigFactory }
import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.PortAccess

object Urls {
  val config: Config = ConfigFactory.load()
  val protocol: String = if (config.getBoolean("dashboard.use-https")) "https://" else "http://"
  val rootDomain: String = config.getString("drt.domain")

  val rootUrl = s"$protocol$rootDomain"

  def portCodeFromUrl(lhrUrl: String): Option[String] = {
    val maybeDomain = lhrUrl.split("://").reverse.headOption
    val maybePortCodeString = maybeDomain.flatMap(_.toUpperCase.split("\\.").toList.headOption)
    maybePortCodeString.flatMap(Roles.parse).collect {
      case pa: PortAccess => pa.name
    }
  }

  def logoutUrlForPort(port: String): String = {
    val portUrl = urlForPort(port)
    s"$portUrl/oauth/logout?redirect=$portUrl"
  }

  def urlForPort(port: String): String = s"$protocol${port.toLowerCase}.$rootDomain"
}
