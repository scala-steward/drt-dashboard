package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.JsonSupport
import uk.gov.homeoffice.drt.auth.Roles.BorderForceStaff
import uk.gov.homeoffice.drt.db.{ MockUserDao, User }
import uk.gov.homeoffice.drt.services.UserService

import java.sql.Timestamp
import java.time.Instant

class UserRoutesSpec extends Specification with Specs2RouteTest with JsonSupport {
  val testKit: ActorTestKit = ActorTestKit()

  implicit val sys = testKit.system

  val stringToLocalDateTime: String => Instant = dateString => Instant.parse(dateString)

  val userService = new UserService(new MockUserDao())
  userService.upsertUser(User(
    "poise/test1",
    "poise/test1",
    "test1@test.com",
    new Timestamp(stringToLocalDateTime("2022-12-05T10:15:30.00Z").toEpochMilli),
    None,
    None))
  userService.upsertUser(User(
    "poise/test2",
    "poise/test2",
    "test2@test.com",
    new Timestamp(stringToLocalDateTime("2022-12-05T10:15:30.00Z").toEpochMilli),
    None,
    None))

  val routes: Route = UserRoutes(
    "user",
    userService)

  "Request data for user should" >> {
    "Give list of all users accessing drt" >> {
      Get("/user/all") ~>
        RawHeader("X-Auth-Roles", BorderForceStaff.name) ~>
        RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
          responseAs[String] shouldEqual
            """[{"email":"test1@test.com","id":"poise/test1","latest_login":"2022-12-05 10:15:30.0","username":"poise/test1"},{"email":"test2@test.com","id":"poise/test2","latest_login":"2022-12-05 10:15:30.0","username":"poise/test2"}]""".stripMargin
        }
    }
  }
}
