package uk.gov.homeoffice.drt.notifications

import java.util

import uk.gov.homeoffice.drt.authentication.AccessRequest
import uk.gov.service.notify.{ NotificationClient, SendEmailResponse }

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.util.Try

case class EmailNotifications(apiKey: String, accessRequestEmails: List[String]) {
  val client = new NotificationClient(apiKey)

  def sendRequest(requester: String, accessRequest: AccessRequest): List[(String, Try[SendEmailResponse])] = {

    val staffing = if (accessRequest.staffing) "yes" else "no"
    val agreeDeclaration = if (accessRequest.agreeDeclaration) "yes" else "no"

    val manager = if (accessRequest.lineManager.nonEmpty) accessRequest.lineManager else "n/a"

    val portsRequested = if (accessRequest.allPorts) "all ports" else accessRequest.portsRequested.mkString(", ").toUpperCase

    val rccuRegionsRequested = accessRequest.rccuRegionsRequested.mkString(", ").toUpperCase

    val personalisation: util.Map[String, String] = Map(
      "requester" -> requester,
      "portList" -> portsRequested,
      "rccuRegion" -> rccuRegionsRequested,
      "staffing" -> staffing,
      "lineManager" -> manager,
      "agreeDeclaration" -> agreeDeclaration).asJava

    accessRequestEmails.map { accessRequestEmail =>
      val maybeResponse = Try(client.sendEmail(
        "5f34d7bb-293f-481c-826b-62661ba8a736",
        accessRequestEmail,
        personalisation,
        ""))
      (accessRequestEmail, maybeResponse)
    }
  }
}
