package uk.gov.homeoffice.drt

//import akka.actor.typed.ActorSystem
import akka.actor.ClassicActorSystemProvider
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.{ HttpHeader, HttpMethods, HttpRequest, HttpResponse }

import scala.concurrent.Future

object DashboardClient {
  def get(uri: String)(implicit system: ClassicActorSystemProvider): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(HttpMethods.GET, uri))
  }

  def getWithRoles(uri: String, roles: List[String])(implicit system: ClassicActorSystemProvider): Future[HttpResponse] = {
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
