package uk.gov.homeoffice.drt.keycloak

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.KeyCloakConfig

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

case class TokenData(username: String, creationTime: Long, keyCloakAuthToken: KeyCloakAuthToken)

object KeyCloakAuthTokenService {
  trait Token

  final case class GetToken(replyTo: ActorRef[KeyCloakAuthToken]) extends Token

  case class SetToken(manageUserToken: Option[TokenData]) extends Token

  private var manageUserToken: Option[TokenData] = None

  def getKeyClockClient(url: String, keyCloakAuthToken: KeyCloakAuthToken)
                       (implicit system: ActorSystem, ec: ExecutionContext): KeyCloakClient = {
    val requestToEventualResponse: HttpRequest => Future[HttpResponse] = request => Http()(system).singleRequest(request)
    KeyCloakClient(keyCloakAuthToken.accessToken, url, requestToEventualResponse)
  }

  def getTokenBehavior(keyCloakConfig: KeyCloakConfig, manageUsername: String, managePassword: String): Behavior[Token] = {

    Behaviors.setup { context: ActorContext[Token] =>
      implicit val system = context.system.classicSystem
      implicit val ec: ExecutionContext = system.dispatcher
      val sendHttpRequest: HttpRequest => Future[HttpResponse] = request => Http().singleRequest(request)

      val authClient = KeyCloakAuth(
        keyCloakConfig.tokenUrl,
        keyCloakConfig.clientId,
        keyCloakConfig.clientSecret,
        sendHttpRequest,
      )

      Behaviors.receiveMessage {
        case GetToken(replyTo) =>
          context.log.info("GetToken by {}", replyTo)
          getUserToken(manageUsername, managePassword, authClient.getToken).map {
            case token: KeyCloakAuthToken => replyTo ! token
            case error: KeyCloakAuthError =>
              context.log.error(s"Failed to get keycloak token: ${error.error}")
          }
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

  def getUserToken(username: String,
                   password: String,
                   getToken: (String, String) => Future[KeyCloakAuthResponse],
                  ): Future[KeyCloakAuthResponse] =
    manageUserToken match {
      case Some(tokenData) if !isTokenExpired(tokenData) => Future.successful(tokenData.keyCloakAuthToken)
      case _ => getToken(username, password)
    }
}

