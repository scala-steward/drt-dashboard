package uk.gov.homeoffice.drt

import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.{ Delete, Get, Post }
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model._
import uk.gov.homeoffice.drt.auth.Roles.Role

import scala.concurrent.Future

object DashboardClient {

  def get(uri: String)(implicit system: ClassicActorSystemProvider): Future[HttpResponse] = {
    Http().singleRequest(HttpRequest(HttpMethods.GET, uri))
  }

  def userDetailDrtApi(uri: String, roles: Iterable[Role], keyCloakToken: String, method: String)(implicit system: ClassicActorSystemProvider): Future[HttpResponse] = {
    val keyCloakHeader = HttpHeader.parse("X-Forwarded-Access-Token", keyCloakToken) match {
      case Ok(header, _) => Option(header)
      case _ => None
    }

    val httpRequest: HttpRequest => Future[HttpResponse] =
      request => Http()
        .singleRequest(request
          .withHeaders(keyCloakHeader.toList ::: rolesToRoleHeader(roles)))

    method match {
      case "GET" => httpRequest(Get(uri))
      case "POST" => httpRequest(Post(uri))
    }

  }

  def getWithRoles(uri: String, roles: Iterable[Role])(implicit system: ClassicActorSystemProvider): Future[HttpResponse] =
    Http().singleRequest(Get(uri).withHeaders(rolesToRoleHeader(roles)))

  def postWithRoles(uri: String, json: String, roles: Iterable[Role])(implicit system: ClassicActorSystemProvider): Future[HttpResponse] =

    Http().singleRequest(Post(uri, HttpEntity(ContentTypes.`text/plain(UTF-8)`, json)).withHeaders(rolesToRoleHeader(roles)))

  def deleteWithRoles(uri: String, roles: Iterable[Role])(implicit system: ClassicActorSystemProvider): Future[HttpResponse] =
    Http().singleRequest(Delete(uri).withHeaders(rolesToRoleHeader(roles)))

  def rolesToRoleHeader(roles: Iterable[Role]): List[HttpHeader] = {
    val roleHeader: Option[HttpHeader] = HttpHeader
      .parse("X-Forwarded-Groups", roles.map(_.name.toLowerCase).mkString(",")) match {
        case Ok(header, _) => Option(header)
        case _ => None
      }
    roleHeader.toList
  }

}
