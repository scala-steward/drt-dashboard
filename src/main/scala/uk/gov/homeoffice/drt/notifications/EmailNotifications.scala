package uk.gov.homeoffice.drt.notifications

import java.util

import uk.gov.homeoffice.drt.authentication.AccessRequest
import uk.gov.service.notify.{NotificationClient, SendEmailResponse}

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.util.Try

case class EmailNotifications(apiKey: String, accessRequestEmails: List[String]) {
  val client = new NotificationClient(apiKey)

  val accessRequestEmailTemplateId = "5f34d7bb-293f-481c-826b-62661ba8a736"

  val accessRequestLineManagerNotificationEmailTemplateId = "c80595c3-957a-4310-a419-b2f254df3909"

  def getFirstName(email: String): String = {
    Try(email.split("\\.").head.toLowerCase.capitalize).getOrElse(email)
  }

  def sendRequest(requester: String, accessRequest: AccessRequest): List[(String, Try[SendEmailResponse])] = {

    val staffing = if (accessRequest.staffing) "yes" else "no"
    val agreeDeclaration = if (accessRequest.agreeDeclaration) "yes" else "no"

    val getTextForField: String => String = string => if (string.nonEmpty) string else "n/a"

    val manager = getTextForField(accessRequest.lineManager)

    val portsRequested =
      if (accessRequest.rccOption == "port")
        if (accessRequest.allPorts) "all ports" else accessRequest.portsRequested.mkString(", ").toUpperCase
      else "n/a"

    val rccuRegionsRequested = if (accessRequest.rccOption == "rccu") accessRequest.regionsRequested.mkString(", ").toUpperCase else "n/a"

    val personalisation: util.Map[String, String] = Map(
      "requesterUsername" -> getFirstName(requester),
      "lineManagerUsername" -> getFirstName(manager),
      "requester" -> requester,
      "portList" -> portsRequested,
      "rccuRegion" -> rccuRegionsRequested,
      "staffing" -> staffing,
      "lineManager" -> manager,
      "agreeDeclaration" -> agreeDeclaration,
      "portOrRegionText" -> getTextForField(accessRequest.portOrRegionText),
      "staffText" -> getTextForField(accessRequest.staffText)).asJava

    accessRequestEmails.map { accessRequestEmail =>
      val maybeResponse: Try[SendEmailResponse] = Try(client.sendEmail(
        accessRequestEmailTemplateId,
        accessRequestEmail,
        personalisation,
        ""))
      (accessRequestEmail, maybeResponse)
    }.flatMap { accessEmailResponse =>
      if (accessRequest.lineManager.nonEmpty && (accessRequest.staffing || accessRequest.allPorts || accessRequest.portsRequested.size > 1 || accessRequest.regionsRequested.size > 1)) {
        val managerAccessEmailResponse: Try[SendEmailResponse] = Try(client.sendEmail(
          accessRequestLineManagerNotificationEmailTemplateId,
          manager,
          personalisation,
          ""))
        List(
          (manager, managerAccessEmailResponse),
          accessEmailResponse)
      } else {
        List(accessEmailResponse)
      }

    }
  }
}
