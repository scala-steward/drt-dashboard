package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model.MessageEntity
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny
import uk.gov.homeoffice.drt.keycloak.KeyCloakAuthToken
import uk.gov.homeoffice.drt.routes.UserRoutes.tokenFormat
import uk.gov.homeoffice.drt.routes.api.v1.AuthApiV1Routes.Credentials

import scala.concurrent.{ExecutionContextExecutor, Future}

class AuthApiV1RoutesTest extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  "A post request with a valid username and password should return a token" in {
    val token = KeyCloakAuthToken("access-token", 3600, 3600, "refresh-token", "bearer", 0, "session-state", "scope")
    val routes = AuthApiV1Routes((_, _) => Future.successful(token))
    val credentials = Credentials("username", "password")

    val credsEntity = Marshal(credentials).to[MessageEntity].futureValue
    Post("/auth/token").withEntity(credsEntity) ~> routes ~> check {
      responseAs[String] shouldEqual token.toJson.compactPrint
    }
  }
}
