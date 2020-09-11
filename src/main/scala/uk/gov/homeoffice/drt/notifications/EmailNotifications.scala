package uk.gov.homeoffice.drt.notifications

import java.util

import uk.gov.homeoffice.drt.authentication.AccessRequest
import uk.gov.service.notify.{ NotificationClient, SendEmailResponse }

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.util.Try

case class EmailNotifications(apiKey: String, accessRequestEmail: String) {
  val client = new NotificationClient(apiKey)

  def sendRequest(requester: String, accessRequest: AccessRequest): Try[SendEmailResponse] = {
    val staffing = if (accessRequest.staffing) "yes" else "no"
    val manager = if (accessRequest.lineManager.nonEmpty) accessRequest.lineManager else "n/a"
    val personalisation: util.Map[String, String] = Map(
      "requester" -> requester,
      "portList" -> accessRequest.portsRequested.mkString(", ").toUpperCase,
      "staffing" -> staffing,
      "lineManager" -> manager).asJava

    Try(client.sendEmail(
      "4c73ba75-87d5-42d7-b6d2-7ff557ae65ed",
      accessRequestEmail,
      personalisation,
      ""))
  }
}
