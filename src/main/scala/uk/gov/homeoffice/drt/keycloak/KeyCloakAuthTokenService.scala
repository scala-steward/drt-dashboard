package uk.gov.homeoffice.drt.keycloak

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorRef, ActorSystem, Behavior }
import uk.gov.homeoffice.drt.KeyClockConfig
import uk.gov.homeoffice.drt.http.ProdSendAndReceive
import uk.gov.homeoffice.drt.schedule.Command
import uk.gov.homeoffice.drt.schedule.UserTracking.KeyCloakToken

import java.time.Instant
import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor, Future }

case class TokenData(username: String, creationTime: Long, keyCloakAuthToken: KeyCloakAuthToken)

object KeyCloakAuthTokenService {
  trait Token

  final case class GetToken(replyTo: ActorRef[Command]) extends Token

  case class SetToken(manageUserToken: Option[TokenData]) extends Token

  private var manageUserToken: Option[TokenData] = None

  def getKeyClockClient(url: String, keyCloakAuthToken: KeyCloakAuthToken)(implicit actorSystem: ActorSystem[Nothing], ec: ExecutionContext): KeycloakClient with ProdSendAndReceive = {
    new KeycloakClient(keyCloakAuthToken.accessToken, url) with ProdSendAndReceive
  }

  def getTokenBehavior(keyClockConfig: KeyClockConfig, manageUsername: String, managePassword: String): Behavior[Token] = {
    Behaviors.setup { context: ActorContext[Token] =>
      Behaviors.receiveMessage {
        case GetToken(replyTo) =>
          context.log.info("GetToken by {}", replyTo)
          implicit val ec: ExecutionContextExecutor = context.system.executionContext
          implicit val system: ActorSystem[Nothing] = context.system
          getManageUserToken(keyClockConfig, manageUsername, managePassword, context).map(KeyCloakToken).map(replyTo ! _)
          Behaviors.same

        case SetToken(token: Option[TokenData]) =>
          manageUserToken = token
          Behaviors.same

      }
    }
  }

  def isTokenExpired(tokenData: TokenData): Boolean = {
    Instant.now().getEpochSecond - (tokenData.creationTime + tokenData.keyCloakAuthToken.expiresIn) > 0
  }

  def getTokenFromFuture(manageUsername: String, keyCloakAuthTokenF: Future[KeyCloakAuthResponse], context: ActorContext[Token])(implicit actorSystem: ActorSystem[Nothing]): Future[KeyCloakAuthToken] = {
    implicit val ec: ExecutionContextExecutor = actorSystem.executionContext
    keyCloakAuthTokenF.map { keyCloakAuthResponse =>
      val KeyCloakAuthToken = keyCloakAuthResponse.asInstanceOf[KeyCloakAuthToken]
      context.self ! SetToken(Some(TokenData(manageUsername, Instant.now().getEpochSecond, KeyCloakAuthToken)))
      KeyCloakAuthToken
    }
  }

  def getManageUserToken(keyClockConfig: KeyClockConfig, manageUsername: String, managePassword: String, context: ActorContext[Token])(implicit actorSystem: ActorSystem[Nothing]): Future[KeyCloakAuthToken] = {
    manageUserToken match {
      case Some(tokenData) => if (isTokenExpired(tokenData)) {
        getTokenFromFuture(manageUsername, getUserToken(keyClockConfig, manageUsername, managePassword), context)
      } else Future.successful(tokenData.keyCloakAuthToken)
      case None =>
        getTokenFromFuture(manageUsername, getUserToken(keyClockConfig, manageUsername, managePassword), context)
    }
  }

  def getUserToken(keyClockConfig: KeyClockConfig, username: String, password: String)(implicit actorSystem: ActorSystem[Nothing]): Future[KeyCloakAuthResponse] = {
    val authClient = new KeyCloakAuth(
      keyClockConfig.tokenUrl,
      keyClockConfig.clientId,
      keyClockConfig.clientSecret) with ProdSendAndReceive
    authClient.getToken(username, password)
  }

}

