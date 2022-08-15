package uk.gov.homeoffice.drt.notifications

import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.authentication.AccessRequest

import scala.util.Failure

class NotificationsSpec extends Specification {
  private val config: Config = ConfigFactory.load()
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")
  val recipient: List[String] = config.getString("dashboard.notifications.access-request-emails").split(",").toList

  val notifications: EmailNotifications = EmailNotifications(apiKey, recipient)

  "Given a gov notify client" >> {
    "When I send an email with the correct personalisation tokens" >> {
      "Then I should receive a positive response" >> {
        skipped("Integration test")
        val someFailures = notifications.sendRequest(
          "drtwannabe@somewhere.com",
          AccessRequest(Set("BHX, EMA"), staffing = true, allPorts = false, rccuRegionsRequested = Set("North", "South"), lineManager = "", agreeDeclaration = true))
          .exists {
            case (_, Failure(_)) => true
            case _ => false
          }

        someFailures === false
      }
    }
  }
}
