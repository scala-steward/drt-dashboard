package uk.gov.homeoffice.drt.notifications

import java.util

import akka.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import uk.gov.service.notify.{ NotificationClient, SendEmailResponse }

import scala.collection.JavaConverters._
import scala.util.Try

class NotificationsSpec extends Specification with Specs2RouteTest {
  val apiKey: String = ConfigFactory.load().getString("dashboard.notifications.api-key")

  val client = new NotificationClient(apiKey)

  "Given a gov notify client" >> {
    "When I send an email with the correct personalisation tokens" >> {
      "Then I should receive a positive response" >> {
        val personalisation: util.Map[String, String] = Map(
          "requestee" -> "ringo@albumsnaps.com",
          "portList" -> "BHX, EMA").asJava

        val sendTry = Try(client.sendEmail(
          "4c73ba75-87d5-42d7-b6d2-7ff557ae65ed",
          "DRTPoiseTeam@homeoffice.gov.uk",
          personalisation,
          ""))

        sendTry.isSuccess === true
      }
    }
  }

}
