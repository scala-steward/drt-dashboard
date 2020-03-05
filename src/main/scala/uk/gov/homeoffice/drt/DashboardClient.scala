package uk.gov.homeoffice.drt

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpHeader, HttpMethods, HttpRequest, HttpResponse }
import akka.http.scaladsl.unmarshalling.Unmarshal
import uk.gov.homeoffice.DrtDashboardApp.{ ciriumDataUri, log, portCodes }
import uk.gov.homeoffice.cirium.services.health.CiriumAppHealthSummary
import uk.gov.homeoffice.drt.services.drt.FeedSourceStatus
import uk.gov.homeoffice.drt.services.drt.JsonSupport._

import scala.concurrent.Future

object DashboardClient {
  def get(uri: String)(implicit system: ActorSystem): Future[HttpResponse] = {
    Http()
      .singleRequest(HttpRequest(HttpMethods.GET, uri))
  }

  def getWithRoles(uri: String, roles: List[String])(implicit system: ActorSystem): Future[HttpResponse] = {
    val roleHeader = HttpHeader.parse("X-Auth-Roles", roles.mkString(",")) match {
      case Ok(header, _) => Option(header)
      case _ => None
    }

    Http()
      .singleRequest(
        HttpRequest(
          HttpMethods.GET, uri, roleHeader.toList))
  }

}
