package uk.gov.homeoffice.drt.keycloak

import akka.http.scaladsl.model.HttpResponse
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.authentication.KeyCloakUser

import scala.concurrent.{ExecutionContext, Future}

trait IKeycloakService {
  implicit val ec: ExecutionContext

  def getUsersForEmail(email: String): Future[Option[KeyCloakUser]]

  def removeUser(userId: String): Future[HttpResponse]

  def addUserToGroup(userId: String, group: String): Future[HttpResponse]

  def logout(username: String): Future[Option[Future[HttpResponse]]]
}

case class KeycloakService(keycloakClient: KeycloakClient)(implicit val ec: ExecutionContext) extends IKeycloakService {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def getUsersForEmail(email: String): Future[Option[KeyCloakUser]] = {
    keycloakClient.getUsersForEmail(email)
  }

  def removeUser(userId: String): Future[HttpResponse] = {
    keycloakClient.removeUser(userId)
  }

  def addUserToGroup(userId: String, group: String): Future[HttpResponse] = {
    val keyCloakGroup = keycloakClient.getGroups.map(a => a.find(_.name == group))

    keyCloakGroup.flatMap {
      case Some(kcg) =>
        val response: Future[HttpResponse] = keycloakClient.addUserToGroup(userId, kcg.id)
        response map { r =>
          r.status.intValue match {
            case s if s > 200 && s < 300 =>
              log.info(s"Added group $group  to userId $userId , with response status: ${r.status}  $r")
              r
            case _ => throw new Exception(s"unable to add group $group to userId $userId response from keycloak $response")
          }
        }

      case _ =>
        log.error(s"Unable to add $userId to $group")
        Future.failed(new Exception(s"Unable to add $userId to $group"))
    }
  }

  def logout(username: String): Future[Option[Future[HttpResponse]]] = {
    keycloakClient.getUsersForUsername(username)
      .map(u =>
        u.map { ud =>
          keycloakClient.logoutUser(ud.id)
        })
  }

}

