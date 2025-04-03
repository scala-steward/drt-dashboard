package uk.gov.homeoffice.drt.routes

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt._
import uk.gov.homeoffice.drt.alerts.MultiPortAlertJsonSupport
import uk.gov.homeoffice.drt.authentication._


object ConfigRoutes extends MultiPortAlertJsonSupport
  with UserJsonSupport
  with ClientConfigJsonFormats
  with ClientUserAccessDataJsonSupport {

  val log: Logger = LoggerFactory.getLogger(getClass)


  def apply(clientConfig: ClientConfig): Route =
    (get & path("config")) {
      headerValueByName("X-Forwarded-Groups") { _ =>
        complete(clientConfig)
      }
    }
}
