package uk.gov.homeoffice.drt

import org.apache.pekko.actor.ClassicActorSystemProvider
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.client.RequestBuilding.{Delete, Get, Post}
import org.apache.pekko.http.scaladsl.model._
import uk.gov.homeoffice.drt.HttpClient.rolesToRoleHeader
import uk.gov.homeoffice.drt.auth.Roles.Role

import scala.concurrent.Future

object DashboardClient {

  def get(uri: String)(implicit system: ClassicActorSystemProvider): Future[HttpResponse] =
    Http().singleRequest(HttpRequest(HttpMethods.GET, uri))

  def getWithRoles(uri: String, roles: Iterable[Role])(implicit system: ClassicActorSystemProvider): Future[HttpResponse] =
    Http().singleRequest(Get(uri).withHeaders(rolesToRoleHeader(roles)))

  def postWithRoles(uri: String, json: String, roles: Iterable[Role])(implicit system: ClassicActorSystemProvider): Future[HttpResponse] =
    Http().singleRequest(Post(uri, HttpEntity(ContentTypes.`text/plain(UTF-8)`, json)).withHeaders(rolesToRoleHeader(roles)))

  def deleteWithRoles(uri: String, roles: Iterable[Role])(implicit system: ClassicActorSystemProvider): Future[HttpResponse] =
    Http().singleRequest(Delete(uri).withHeaders(rolesToRoleHeader(roles)))

}
