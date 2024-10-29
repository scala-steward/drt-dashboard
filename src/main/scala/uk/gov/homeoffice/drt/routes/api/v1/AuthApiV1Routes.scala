package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.{Logger, LoggerFactory}
import spray.json.RootJsonFormat
import uk.gov.homeoffice.drt.db
import uk.gov.homeoffice.drt.keycloak._

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object AuthApiV1Routes extends db.UserAccessRequestJsonSupport with KeyCloakAuthTokenParserProtocol {
  val log: Logger = LoggerFactory.getLogger(getClass)

  trait JsonResponse {
    def startTime: String

    def endTime: String

    def ports: Seq[String]
  }

  case class Credentials(username: String, password: String)

  implicit val credentialsJsonFormat: RootJsonFormat[Credentials] = jsonFormat2(Credentials)

  def apply(getKeyCloakToken: (String, String) => Future[KeyCloakAuthResponse])
           (implicit ec: ExecutionContextExecutor): Route = {
    (post & path("auth" / "token")) {

      entity(as[Credentials]) { case Credentials(username, password) =>
        val eventualToken: Future[KeyCloakAuthToken] = getKeyCloakToken(username, password).map {
          case token: KeyCloakAuthToken =>
            log.info(s"Successful login to API via keycloak for $username")
            token
          case _: KeyCloakAuthError =>
            throw new Exception(s"Failed login to API via keycloak for $username")
        }

        onComplete(eventualToken) {
          case Success(token) => complete(token)
          case Failure(t) =>
            log.error(t.getMessage)
            complete(InternalServerError)
        }
      }
    }
  }
}

