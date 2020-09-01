package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.authentication.Roles.{ BorderForceStaff, LHR }

class ApiRoutesSpec extends Specification with Specs2RouteTest {
  val routes: Route = ApiRoutes("api", Array("lhr", "stn"))
  "Given a uri with an X-Auth-Roles header with staff access but no port access I should see the internal signup page" >> {
    Get("/api/user") ~> RawHeader("X-Auth-Roles", BorderForceStaff.name) ~> routes ~> check {
      responseAs[String] shouldEqual """{"ports":["LHR"],"email":"ringo@albumsnaps.com"}"""
    }
  }

  "Given a uri with an X-Auth-Roles header with staff & port access and no port query parameter I should see the existing user page to choose a port" >> {
    Get("/api/user") ~> RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> routes ~> check {
      responseAs[String] shouldEqual """{"ports":["LHR"],"email":"ringo@albumsnaps.com"}"""
    }
  }

  "Given a uri with an X-Auth-Roles header and a request for config I should see some config" >> {
    Get("/api/config") ~> RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> routes ~> check {
      responseAs[String] shouldEqual """{"ports":["lhr","stn"],"domain":"drt.homeoffice.gov.uk"}"""
    }
  }
}
