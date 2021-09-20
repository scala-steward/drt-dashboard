package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.{ Location, RawHeader }
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{ Route, StandardRoute }
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.Urls
import uk.gov.homeoffice.drt.auth.Roles.{ BHX, BorderForceStaff, LHR }

class IndexRouteSpec extends Specification with Specs2RouteTest {
  private val config: Config = ConfigFactory.load()
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")
  val rootDomain: String = "some-domain.com"
  val useHttps: Boolean = true

  val urls: Urls = Urls(rootDomain, useHttps)

  val dummyRoute: StandardRoute = complete("dummy")

  val indexRoute: IndexRoute = IndexRoute(
    urls = urls,
    indexResource = complete("the app"),
    directoryResource = dummyRoute,
    staticResourceDirectory = dummyRoute)

  val routes: Route = indexRoute.indexRouteDirectives(RootPathString)

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
    val lhrUrl = urls.urlForPort(LHR.name)
    Get("/") ~>
      RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, BHX.name).mkString(",")) ~>
      RawHeader("Referer", lhrUrl + "/") ~> routes ~> check {
        responseAs[String] shouldEqual "the app"
      }
  }

  "A user with referer uri for LHR, and role access to LHR should get redirected back to LHR' logout url" >> {
    val lhrUrl = urls.urlForPort(LHR.name)
    Get("/?fromPort=lhr") ~>
      RawHeader("X-Auth-Roles", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~> routes ~> check {
        val isTempRedirected = status shouldEqual StatusCodes.TemporaryRedirect
        val isLhrLogoutUrl = header("Location") shouldEqual Option(Location(s"$lhrUrl/oauth/logout?redirect=$lhrUrl"))
        isTempRedirected && isLhrLogoutUrl
      }
  }
}
