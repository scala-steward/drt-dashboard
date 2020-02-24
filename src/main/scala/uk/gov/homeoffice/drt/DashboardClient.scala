package uk.gov.homeoffice.drt

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}

import scala.concurrent.Future

object DashboardClient {
  def get(uri: String)(implicit system: ActorSystem): Future[HttpResponse] = {
    Http()
      .singleRequest(HttpRequest(HttpMethods.GET, uri))
  }
}
