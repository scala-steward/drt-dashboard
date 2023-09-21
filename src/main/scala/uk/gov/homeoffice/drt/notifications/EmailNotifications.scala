package uk.gov.homeoffice.drt.notifications

import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.authentication.{AccessRequest, ClientUserRequestedAccessData}
import uk.gov.homeoffice.drt.notifications.templates.AccessRequestTemplates.{accessGrantedTemplateId, requestTemplateId, lineManagerNotificationTemplateId}
import uk.gov.service.notify.{NotificationClientApi, SendEmailResponse}
import uk.gov.homeoffice.drt.db.{DropInDao, DropInRow}
import uk.gov.service.notify.{NotificationClientApi, SendEmailResponse}

import java.util
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Try

case class EmailNotifications(accessRequestEmails: List[String], client: NotificationClientApi) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val accessRequestEmailTemplateId = "5f34d7bb-293f-481c-826b-62661ba8a736"

  val accessRequestLineManagerNotificationEmailTemplateId = "c80595c3-957a-4310-a419-b2f254df3909"

  val accessGrantedTemplateId = "12e36257-c485-4e13-af4f-2293d2dd34a6"

  val inactiveUserNotificationTemplateId = "58224cba-7313-4dc9-96f3-d8cb34550ec8"

  val revokeAccessTemplateId = "a50b8424-a8d8-49fe-b826-381623f9aace"

  val dropInReminderTemplateId = "73c1d3a7-9f52-4ccc-a0c4-4c2837b86bf9"

  def getFirstName(email: String): String = {
    Try(email.split("\\.").head.toLowerCase.capitalize).getOrElse(email)
  }

  def getLink(curad: ClientUserRequestedAccessData, domain: String): String = {
    if (curad.allPorts || curad.regionsRequested.length > 4 || curad.portsRequested.length > 4)
      s"https://$domain"
    else
      s"https://${curad.portsRequested.trim.toLowerCase()}.$domain/"
  }

  def sendDropInReminderEmail(email: String, dropIn: DropInRow, teamEmail: String) = {
    import DropInDao._
    val personalisation = Map(
      "teamEmail" -> teamEmail,
      "requesterUsername" -> getFirstName(email),
      "title" -> dropIn.title,
      "dropInDate" -> getDate(dropIn.startTime),
      "startTime" -> getStartTime(dropIn.startTime),
      "endTime" -> getEndTime(dropIn.endTime),
      "meetingLink" -> dropIn.meetingLink.getOrElse(""),
    ).asJava

    Try(client.sendEmail(
      dropInReminderTemplateId,
      email,
      personalisation, "Drop-In Reminder")).recover {
      case e => log.error(s"Error sending drop-in registration email to user $email", e)
    }

  }

  def sendAccessGranted(clientUserRequestedAccessData: ClientUserRequestedAccessData, domain: String, teamEmail: String): Try[SendEmailResponse] = {
    val personalisation: util.Map[String, String] =
      Map(
        "requesterUsername" -> getFirstName(clientUserRequestedAccessData.email),
        "link" -> getLink(clientUserRequestedAccessData, domain),
      ).asJava
    Try(client.sendEmail(
      accessGrantedTemplateId,
      clientUserRequestedAccessData.email,
      personalisation,
      "access granted")
    ).recover {
      case e => log.error(s"Error while sending email to requester ${clientUserRequestedAccessData.email} for grant access confirmation")
        throw e
    }
    Try(client.sendEmail(
      accessGrantedTemplateId,
      teamEmail,
      personalisation,
      "access granted bcc")
    ).recover {
      case e => log.error(s"Error while sending bcc email to team $teamEmail for requester ${clientUserRequestedAccessData.email} for grant access confirmation")
        throw e
    }
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
        requestTemplateId,
        accessRequestEmail,
        personalisation,
        ""))
      (accessRequestEmail, maybeResponse)
    }.flatMap { accessEmailResponse =>
      if (accessRequest.lineManager.nonEmpty && (accessRequest.staffing || accessRequest.allPorts || accessRequest.portsRequested.size > 1 || accessRequest.regionsRequested.size > 1)) {
        val managerAccessEmailResponse: Try[SendEmailResponse] = Try(client.sendEmail(
          lineManagerNotificationTemplateId,
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

  def sendUserInactivityEmailNotification(email: String, domain: String, teamEmail: String, templateId: String, reference: String): Try[SendEmailResponse] = {
    val personalisation: util.Map[String, String] = {
      Map(
        "emailAddress" -> email,
        "requesterUsername" -> getFirstName(email),
        "link" -> s"https://$domain",
        "teamEmail" -> teamEmail
      ).asJava
    }
    Try(client.sendEmail(
      templateId,
      email,
      personalisation,
      reference)
    ).recover {
      case e => log.error(s"Error while sending email to user $email for $reference notification", e)
        throw e
    }
  }
}
