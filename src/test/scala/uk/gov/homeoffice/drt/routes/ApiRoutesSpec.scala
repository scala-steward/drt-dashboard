package uk.gov.homeoffice.drt.routes

import akka.actor
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import akka.testkit.TestKit
import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mutable.{ Specification, SpecificationLike }
import uk.gov.homeoffice.drt.auth.Roles.{ BorderForceStaff, LHR }
import uk.gov.homeoffice.drt.notifications.EmailNotifications

//class ApiRoutesSpec extends TestKit(ActorSystem[Nothing]("dashboard-test")) with SpecificationLike with Specs2RouteTest {
//
//  implicit val sys: actor.ActorSystem = system
//
//  private val config: Config = ConfigFactory.load()
//  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")
//
//  val routes: Route = ApiRoutes("api", Array("lhr", "stn"), "somedomain.com", EmailNotifications(apiKey, List("access-requests@drt")))
//
//  "Given a uri accessed by a user with an email but no port access, I should see an empty port list and their email address in JSON" >> {
//    Get("/api/user") ~>
//      RawHeader("X-Auth-Roles", BorderForceStaff.name) ~>
//      RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
//        responseAs[String] shouldEqual """{"ports":[],"email":"my@email.com"}"""
//      }
//  }
//
//  "Given a uri accessed by a user with an email and LHR port access, I should see LHR in the port list and their email address in JSON" >> {
//    Get("/api/user") ~>
//      RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~>
//      RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
//        responseAs[String] shouldEqual """{"ports":["LHR"],"email":"my@email.com"}"""
//      }
//  }
//
//  "Given an api request for config, I should see a JSON response containing the config passed to ApiRoutes" >> {
//    Get("/api/config") ~>
//      RawHeader("X-Auth-Roles", "") ~>
//      RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
//        responseAs[String] shouldEqual """{"ports":["lhr","stn"],"domain":"somedomain.com"}"""
//      }
//  }
//}
