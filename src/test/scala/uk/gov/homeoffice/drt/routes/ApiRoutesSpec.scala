package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.specs2.mutable.Specification
import spray.json._
import uk.gov.homeoffice.drt.auth.Roles.{BorderForceStaff, LHR}
import uk.gov.homeoffice.drt.db.{MockUserDao, User}
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.services.UserService
import uk.gov.homeoffice.drt.{ClientConfig, ClientConfigJsonFormats, JsonSupport}

import java.sql.Timestamp
import java.util.Date
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ApiRoutesSpec extends Specification with Specs2RouteTest with JsonSupport with ClientConfigJsonFormats {
  val testKit: ActorTestKit = ActorTestKit()

  implicit val sys: ActorSystem[Nothing] = testKit.system

  private val config: Config = ConfigFactory.load()
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")

  val clientConfig: ClientConfig = ClientConfig(Seq(PortRegion.North), "somedomain.com", "test@test.com")
  val neboRoutes: NeboUploadRoutes = NeboUploadRoutes(List(), new MockHttpClient(HttpResponse()))
  val userService = new UserService(new MockUserDao)
  val routes: Route = ApiRoutes(
    "api",
    clientConfig,
    neboRoutes.route, userService)

  "Given a uri accessed by a user with an email but no port access, I should see an empty port list and their email address in JSON" >> {
    Get("/api/user") ~>
      RawHeader("X-Auth-Roles", BorderForceStaff.name) ~>
      RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
      responseAs[String] shouldEqual """{"ports":[],"roles":["border-force-staff"],"email":"my@email.com"}"""
    }
  }

  "Given a uri accessed by a user with an email and LHR port access, I should see LHR in the port list and their email address in JSON" >> {
    Get("/api/user") ~>
      RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~>
      RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
      responseAs[String] shouldEqual """{"ports":["LHR"],"roles":["border-force-staff","LHR"],"email":"my@email.com"}"""
    }
  }

  "Given an api request for config, I should see a JSON response containing the config passed to ApiRoutes" >> {
    Get("/api/config") ~>
      RawHeader("X-Auth-Roles", "") ~>
      RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
      responseAs[JsValue] shouldEqual clientConfig.toJson
    }
  }

  "When user tracking is received with expected headers, user details is present in user table" >> {
    Get("/api/track-user") ~>
      RawHeader("X-Auth-Roles", "") ~>
      RawHeader("X-Auth-username", "my") ~>
      RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
      responseAs.status shouldEqual OK
      val user = Await.result(userService.getUsers(), 1.seconds).head
      user.id shouldEqual "my"
      user.username shouldEqual "my"
      user.email shouldEqual "my@email.com"
      new Timestamp(new Date().getTime).getTime - user.latest_login.getTime < 100
    }
  }
}
