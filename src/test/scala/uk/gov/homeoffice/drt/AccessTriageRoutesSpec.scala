package uk.gov.homeoffice.drt

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.authentication.Roles.{ BorderForceStaff, LHR }
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.routes.AccessTriageComponents.{ SimpleComponent, UserComponent }
import uk.gov.homeoffice.drt.routes.ApiRoutes

class AccessTriageRoutesSpec extends Specification with Specs2RouteTest {
  val externalUserComponent: SimpleComponent = () => "External"
  val internalUserComponent: UserComponent = (user: User) => "Internal"
  val existingUserComponent: UserComponent = (user: User) => "Existing"

  val routes: (String, Array[String]) => Route = ApiRoutes(externalUserComponent, internalUserComponent, existingUserComponent)

  "Given a uri with no X-Auth-Roles header I should see the external signup page" >> {
    Get("/here") ~> routes("here", Array()) ~> check {
      responseAs[String] shouldEqual "External"
    }
  }

  "Given a uri with an X-Auth-Roles header with no staff access I should see the internal signup page" >> {
    Get("/here") ~> RawHeader("X-Auth-Roles", "") ~> routes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Internal"
    }
  }

  "Given a uri with an X-Auth-Roles header with staff access but no port access I should see the internal signup page" >> {
    Get("/here") ~> RawHeader("X-Auth-Roles", BorderForceStaff.name) ~> routes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Internal"
    }
  }

  "Given a uri with an X-Auth-Roles header with staff & port access and no port query parameter I should see the existing user page to choose a port" >> {
    Get("/here") ~> RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> routes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Existing"
    }
  }
}
