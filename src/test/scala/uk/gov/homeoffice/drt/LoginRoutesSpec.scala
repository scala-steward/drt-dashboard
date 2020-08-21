package uk.gov.homeoffice.drt

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.authentication.Roles.{ BorderForceStaff, LHR }
import uk.gov.homeoffice.drt.routes.LoginRoutes

class LoginRoutesSpec extends Specification with Specs2RouteTest {
  "Given a uri with no x-auth-roles header" >> {
    Get("/here") ~> LoginRoutes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Access denied"
    }
  }

  "Given a uri with an x-auth-roles header with no staff access" >> {
    Get("/here") ~> RawHeader("x-auth-roles", "") ~> LoginRoutes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Please request access to DRT"
    }
  }

  "Given a uri with an x-auth-roles header with staff access but no port access" >> {
    Get("/here") ~> RawHeader("x-auth-roles", BorderForceStaff.name) ~> LoginRoutes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Please request access to a DRT port"
    }
  }

  "Given a uri with an x-auth-roles header with staff & port access and no port query parameter" >> {
    Get("/here") ~> RawHeader("x-auth-roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> LoginRoutes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Choose a port"
    }
  }

  "Given a uri with an x-auth-roles header with staff & port access and an invalid port query parameter" >> {
    Get("/here?port=xxx") ~> RawHeader("x-auth-roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> LoginRoutes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Invalid port"
    }
  }

  "Given a uri with an x-auth-roles header with staff & port access but a port query parameter for an inaccessible port" >> {
    Get("/here?port=bhx") ~> RawHeader("x-auth-roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> LoginRoutes("here", Array()) ~> check {
      responseAs[String] shouldEqual "Please request access to bhx"
    }
  }

  "Given a uri with an x-auth-roles header with staff & port access and a port query parameter for an accessible port" >> {
    Get("/here?port=lhr") ~> RawHeader("x-auth-roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> LoginRoutes("here", Array()) ~> check {
      response.status shouldEqual StatusCodes.TemporaryRedirect
    }
  }
}

//
//        if (roles.isEmpty)
//          context.complete("Welcome new user. Choose the ports you'd like access to")
//        else {
//          maybePort match {
//            //              case Some(port) if roles.contains(Roles.parse(port)) => redirect(s"$port.drt-preprod.homeoffice.gov.uk", StatusCodes.Redirection)
//            case None =>
//              context.complete(s"Welcome existing user. Please choose a port from ${roles.collect { case r: PortAccess => r }.mkString(", ")}")
//          }
//        }
//      }
