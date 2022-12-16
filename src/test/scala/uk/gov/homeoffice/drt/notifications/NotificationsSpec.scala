package uk.gov.homeoffice.drt.notifications

import com.typesafe.config.{ Config, ConfigFactory }
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.authentication.AccessRequest
import uk.gov.service.notify.NotificationClient

import scala.util.Failure

class NotificationsSpec extends Specification {
  private val config: Config = ConfigFactory.load()
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")
  val recipient: List[String] = config.getString("dashboard.notifications.access-request-emails").split(",").toList
  val notificationClient = new NotificationClient(apiKey)
  val notifications: EmailNotifications = EmailNotifications(recipient, notificationClient)

  "Given a gov notify client" >> {
    "When I send an email with the correct personalisation tokens" >> {
      "Then I should receive a positive response" >> {
        skipped("Integration test")
        val someFailures = notifications.sendRequest(
          "drtwannabe@somewhere.com",
          AccessRequest(
            portsRequested = Set("BHX, EMA"),
            staffing = true,
            allPorts = false,
            regionsRequested = Set("North", "South"),
            lineManager = "",
            agreeDeclaration = true,
            rccOption = "rccu",
            portOrRegionText = "",
            staffText = ""))
          .exists {
            case (_, Failure(_)) => true
            case _ => false
          }

        someFailures === false
      }
    }

    "Given email with '.' in address" >> {
      "Then First name with capitalise from email pattern" >> {
        val firstName = notifications.getFirstName("firstName.lastName@test.com")
        firstName === "Firstname"
      }
    }
  }
}
