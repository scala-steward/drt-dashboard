package uk.gov.homeoffice.drt.routes

object PortUrl {
  def portCodeFromUrl(lhrUrl: String): Option[String] = {
    val maybeDomain = lhrUrl.split("://").reverse.headOption
    val maybePortCode = maybeDomain.flatMap(_.toUpperCase.split("\\.").toList.headOption)
    maybePortCode
  }

  def logoutUrlForPort(port: String, domain: String): String = {
    val portUrl = urlForPort(port, domain)
    s"$portUrl/oauth/logout?redirect=$portUrl"
  }

  def urlForPort(port: String, domain: String): String =
    s"https://${port.toLowerCase}.$domain"
}
