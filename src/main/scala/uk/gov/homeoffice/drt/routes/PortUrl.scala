package uk.gov.homeoffice.drt.routes

import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.PortAccess

object PortUrl {
  def portCodeFromUrl(lhrUrl: String): Option[String] = {
    val maybeDomain = lhrUrl.split("://").reverse.headOption
    val maybePortCodeString = maybeDomain.flatMap(_.toUpperCase.split("\\.").toList.headOption)
    maybePortCodeString.flatMap(Roles.parse).collect {
      case pa: PortAccess => pa.name
    }
  }

  def logoutUrlForPort(port: String, domain: String): String = {
    val portUrl = urlForPort(port, domain)
    s"$portUrl/oauth/logout?redirect=$portUrl"
  }

  def urlForPort(port: String, domain: String): String =
    s"https://${port.toLowerCase}.$domain"
}
