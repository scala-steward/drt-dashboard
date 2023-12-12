package uk.gov.homeoffice.drt.notifications

import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.authentication.{AccessRequest, ClientUserRequestedAccessData}
import uk.gov.homeoffice.drt.db.{DropInDao, DropInRow, UserAccessRequest}
import uk.gov.homeoffice.drt.notifications.templates.AccessRequestTemplates.{lineManagerNotificationTemplateId, requestTemplateId}
import uk.gov.service.notify.{NotificationClientApi, SendEmailResponse}

import java.util
import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Try

case class EmailNotifications(accessRequestEmails: List[String], client: NotificationClientApi) {
  lazy val log: Logger = LoggerFactory.getLogger(getClass)

  val accessRequestEmailTemplateId = "5f34d7bb-293f-481c-826b-62661ba8a736"

  val accessRequestLineManagerNotificationEmailTemplateId = "c80595c3-957a-4310-a419-b2f254df3909"

  val accessGrantedTemplateId = "1335ae5f-c1fa-490f-98e0-1b54894e8f96"

  val inactiveUserNotificationTemplateId = "58224cba-7313-4dc9-96f3-d8cb34550ec8"

  val revokeAccessTemplateId = "a50b8424-a8d8-49fe-b826-381623f9aace"

  val dropInReminderTemplateId = "73c1d3a7-9f52-4ccc-a0c4-4c2837b86bf9"

  val dropInNotificationTemplateId = "4a71e846-ddac-4b93-bd2d-1d5652dcb8bb"

  def getFirstName(email: String): String = {
    Try(email.split("\\.").head.toLowerCase.capitalize).getOrElse(email)
  }

  def getLink(curad: ClientUserRequestedAccessData, domain: String): String = {
    if (curad.allPorts || curad.regionsRequested.length > 4 || curad.portsRequested.length > 4)
      s"https://$domain"
    else
      s"https://${curad.portsRequested.trim.toLowerCase()}.$domain/"
  }

  val getDropInBookingUrlForAPort: (String, String) => String = (portsString, domain) => s"https://${portsString.split(",").toList.headOption.map(_.toLowerCase()).getOrElse("lhr")}.$domain/#trainingHub/dropInBooking"

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

  def sendDropInNotification(userAccessRequestO: Option[UserAccessRequest], domain: String, teamEmail: String) = {
    userAccessRequestO.map { userAccessRequest =>
      val personalisation: util.Map[String, String] =
        Map(
          "requesterUsername" -> getFirstName(userAccessRequest.email),
          "dropInLink" -> getDropInBookingUrlForAPort(userAccessRequest.portsRequested, domain),
          "teamEmail" -> teamEmail
        ).asJava
      Try(client.sendEmail(
        dropInNotificationTemplateId,
        userAccessRequest.email,
        personalisation,
        "drop-in notification")
      ).recover {
        case e => log.error(s"Error while sending email to requester ${userAccessRequest.email} for drop-in notification")
          throw e
      }
    }.getOrElse(throw new Exception("UserAccessRequest not found"))

  }

  def sendAccessGranted(clientUserRequestedAccessData: ClientUserRequestedAccessData, domain: String, teamEmail: String): Try[SendEmailResponse] = {
    val personalisation: util.Map[String, String] =
      Map(
        "requesterUsername" -> getFirstName(clientUserRequestedAccessData.email),
        "link" -> getLink(clientUserRequestedAccessData, domain),
        "dropInLink" -> getDropInBookingUrlForAPort(clientUserRequestedAccessData.portsRequested, domain),
        "teamEmail" -> teamEmail
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
