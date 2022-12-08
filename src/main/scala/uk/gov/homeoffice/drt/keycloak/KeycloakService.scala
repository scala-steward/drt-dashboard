package uk.gov.homeoffice.drt.keycloak

import uk.gov.homeoffice.drt.authentication.KeyCloakUser

import scala.concurrent.{ ExecutionContext, Future }

class KeycloakService(keycloakClient: KeycloakClient) {

  def getUsersForEmail(email: String): Future[Option[KeyCloakUser]] = {
    keycloakClient.getUsersForEmail(email)
  }

  def removeUser(userId: String) = {
    keycloakClient.removeUser(userId)
  }

  def logout(username: String)(implicit ec: ExecutionContext) = {
    keycloakClient.getUsersForUsername(username)
      .map(u =>
        u.map { ud =>
          keycloakClient.logoutUser(ud.id)
        })
  }

}

