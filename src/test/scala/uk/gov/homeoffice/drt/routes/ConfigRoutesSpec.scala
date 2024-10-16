package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.specs2.mutable.Specification
import spray.json._
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.{ClientConfig, ClientConfigJsonFormats}

class ConfigRoutesSpec extends Specification with Specs2RouteTest with ClientConfigJsonFormats with SprayJsonSupport with DefaultJsonProtocol{
  val testKit: ActorTestKit = ActorTestKit()

  implicit val sys: ActorSystem[Nothing] = testKit.system

  private val config: Config = ConfigFactory.load()
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")

  val clientConfig: ClientConfig = ClientConfig(Seq(PortRegion.North), Map(PortCode("NCL") -> Seq(T1)), "somedomain.com", "test@test.com")
  val routes: Route = ConfigRoutes(clientConfig)
  "Given an api request for config, I should see a JSON response containing the config passed to ApiRoutes" >> {
    Get("/config") ~>
      RawHeader("X-Forwarded-Groups", "") ~>
      RawHeader("X-Forwarded-Email", "my@email.com") ~> routes ~> check {
      responseAs[JsValue] shouldEqual clientConfig.toJson
    }
  }
}
