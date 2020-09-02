package uk.gov.homeoffice.drt.notifications

import akka.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mutable.Specification

import scala.util.{ Failure, Success }

class NotificationsSpec extends Specification with Specs2RouteTest {
  private val config: Config = ConfigFactory.load()
  val apiKey: String = config.getString("dashboard.notifications.api-key")
  val recipient: String = config.getString("dashboard.notifications.access-request-email")

  val notifications: EmailNotifications = EmailNotifications(apiKey, recipient)

  "Given a gov notify client" >> {
    "When I send an email with the correct personalisation tokens" >> {
      "Then I should receive a positive response" >> {
        val success = notifications.sendRequest("drtwannabe@somewhere.com", Seq("BHX, EMA")) match {
          case Success(_) => true
          case Failure(t) =>
            println(s"Failed", t)
        }

        success === true
      }
    }
  }
}
