package uk.gov.homeoffice.drt

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpHeader, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.authentication.Roles.{BorderForceStaff, LHR}
import uk.gov.homeoffice.drt.authentication.{Roles, User}

class LoginRoutesSpec extends Specification with Specs2RouteTest {
  def extractUser: HttpHeader => Option[User] = {
    case RawHeader("x-auth-roles", rolesString) => Option(User(rolesString))
    case _ => None
  }

  val route = pathPrefix("here") {
    get {
      optionalHeaderValue(extractUser) {
        case None =>
          complete("Access denied")
        case Some(user) if !user.hasStaffCredential =>
          complete("Please request access to DRT")
        case Some(user) if !user.hasPortAccess =>
          complete("Please request access to a DRT port")
        case Some(user) =>
          parameter("port".optional) {
            case None =>
              complete("Choose a port")
            case Some(portCode: String) =>
              Roles.parse(portCode) match {
                case None =>
                  complete("Invalid port")
                case Some(portRole) if user.hasRole(portRole) =>
                  redirect(s"$portCode", StatusCodes.TemporaryRedirect)
                case _ =>
                  complete(s"Please request access to $portCode")
              }
          }
      }
    }
  }

  "Given a uri with no x-auth-roles header" >> {
    Get("/here") ~> route ~> check {
      responseAs[String] shouldEqual "Access denied"
    }
  }

  "Given a uri with an x-auth-roles header with no staff access" >> {
    Get("/here") ~> RawHeader("x-auth-roles", "") ~> route ~> check {
      responseAs[String] shouldEqual "Please request access to DRT"
    }
  }

  "Given a uri with an x-auth-roles header with staff access but no port access" >> {
    Get("/here") ~> RawHeader("x-auth-roles", BorderForceStaff.name) ~> route ~> check {
      responseAs[String] shouldEqual "Please request access to a DRT port"
    }
  }

  "Given a uri with an x-auth-roles header with staff & port access and no port query parameter" >> {
    Get("/here") ~> RawHeader("x-auth-roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> route ~> check {
      responseAs[String] shouldEqual "Choose a port"
    }
  }

  "Given a uri with an x-auth-roles header with staff & port access and an invalid port query parameter" >> {
    Get("/here?port=xxx") ~> RawHeader("x-auth-roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> route ~> check {
      responseAs[String] shouldEqual "Invalid port"
    }
  }

  "Given a uri with an x-auth-roles header with staff & port access but a port query parameter for an inaccessible port" >> {
    Get("/here?port=bhx") ~> RawHeader("x-auth-roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> route ~> check {
      responseAs[String] shouldEqual "Please request access to bhx"
    }
  }

  "Given a uri with an x-auth-roles header with staff & port access and a port query parameter for an accessible port" >> {
    Get("/here?port=lhr") ~> RawHeader("x-auth-roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> route ~> check {
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
