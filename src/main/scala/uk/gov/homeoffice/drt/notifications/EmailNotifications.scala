package uk.gov.homeoffice.drt.notifications

import java.util

import uk.gov.service.notify.{ NotificationClient, SendEmailResponse }

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.util.Try

case class EmailNotifications(apiKey: String, accessRequestEmail: String) {
  val client = new NotificationClient(apiKey)

  def sendRequest(requester: String, ports: Iterable[String]): Try[SendEmailResponse] = {
    val personalisation: util.Map[String, String] = Map(
      "requester" -> requester,
      "portList" -> ports.mkString(", ").toUpperCase).asJava

    Try(client.sendEmail(
      "4c73ba75-87d5-42d7-b6d2-7ff557ae65ed",
      accessRequestEmail,
      personalisation,
      ""))
  }
}
