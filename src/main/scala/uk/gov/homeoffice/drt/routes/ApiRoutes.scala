package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt._
import uk.gov.homeoffice.drt.alerts.MultiPortAlertJsonSupport
import uk.gov.homeoffice.drt.authentication._


object ApiRoutes extends MultiPortAlertJsonSupport
  with UserJsonSupport
  with ClientConfigJsonFormats
  with ClientUserAccessDataJsonSupport {

  val log: Logger = LoggerFactory.getLogger(getClass)


  def apply(clientConfig: ClientConfig): Route =
    concat(
      (get & path("config")) {
        headerValueByName("X-Forwarded-Groups") { _ =>
          complete(clientConfig)
        }
      }
    )
}
