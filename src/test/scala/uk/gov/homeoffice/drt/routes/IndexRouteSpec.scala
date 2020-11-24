package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{ Location, RawHeader }
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.auth.Roles.{ BHX, BorderForceStaff, LHR }
import uk.gov.homeoffice.drt.routes.PortUrl.urlForPort

class IndexRouteSpec extends Specification with Specs2RouteTest {
  private val config: Config = ConfigFactory.load()
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")

  private val domain = "somedomain.com"
  val routes: Route = IndexRoute.indexRouteDirectives(complete("the app"), domain)

  "A user with no port access and no referer should see the application" >> {
    Get("/") ~>
      RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name).mkString(",")) ~> routes ~> check {
        responseAs[String] shouldEqual "the app"
      }
  }

  "A user with port access and no referer should see the application" >> {
    Get("/") ~>
      RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> routes ~> check {
        responseAs[String] shouldEqual "the app"
      }
  }

  "A user with referer uri for LHR, and no role access to LHR should see the application" >> {
    val lhrUrl = urlForPort(LHR.name, domain)
    Get("/") ~>
      RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, BHX.name).mkString(",")) ~>
      RawHeader("Referer", lhrUrl + "/") ~> routes ~> check {
        responseAs[String] shouldEqual "the app"
      }
  }

  "A user with referer uri for LHR, and role access to LHR should get redirected back to LHR' logout url" >> {
    val lhrUrl = urlForPort(LHR.name, domain)
    Get("/?fromPort=lhr") ~>
      RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> routes ~> check {
        val isTempRedirected = status shouldEqual StatusCodes.TemporaryRedirect
        val isLhrLogoutUrl = header("Location") shouldEqual Option(Location(s"$lhrUrl/oauth/logout?redirect=$lhrUrl"))
        isTempRedirected && isLhrLogoutUrl
      }
  }
}
