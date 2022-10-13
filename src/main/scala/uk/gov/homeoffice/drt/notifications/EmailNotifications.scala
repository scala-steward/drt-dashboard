package uk.gov.homeoffice.drt.notifications

import org.slf4j.{Logger, LoggerFactory}

import java.util
import uk.gov.homeoffice.drt.authentication.{AccessRequest, ClientUserRequestedAccessData}
import uk.gov.service.notify.{NotificationClient, SendEmailResponse}

import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.util.Try

case class EmailNotifications(apiKey: String, accessRequestEmails: List[String]) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val client = new NotificationClient(apiKey)

  val accessRequestEmailTemplateId = "5f34d7bb-293f-481c-826b-62661ba8a736"

  val accessRequestLineManagerNotificationEmailTemplateId = "c80595c3-957a-4310-a419-b2f254df3909"

  val accessGrantedTemplateId = "12e36257-c485-4e13-af4f-2293d2dd34a6"

  val accessGrantedBccTemplateId = "06e88a3b-2375-4acc-9fde-f2b7afce6bb6"

  def getFirstName(email: String): String = {
    Try(email.split("\\.").head.toLowerCase.capitalize).getOrElse(email)
  }

  def getLink(curad: ClientUserRequestedAccessData, domain: String): String = {
    if (curad.allPorts || curad.regionsRequested.length > 4 || curad.portsRequested.length > 4)
      s"https://$domain"
    else
      s"https://${curad.portsRequested.trim.toLowerCase()}.$domain/"
  }

  def sendAccessGranted(clientUserRequestedAccessData: ClientUserRequestedAccessData, domain: String, teamEmail: String) = {
    val personalisation: util.Map[String, String] =
      Map("requester" -> clientUserRequestedAccessData.email,
        "requesterUsername" -> getFirstName(clientUserRequestedAccessData.email),
        "link" -> getLink(clientUserRequestedAccessData, domain),
      ).asJava
    Try(client.sendEmail(
      accessGrantedTemplateId,
      clientUserRequestedAccessData.email,
      personalisation,
      "access granted")).recover {
      case e => log.error(s"Error while sending email to requester ${clientUserRequestedAccessData.email} for grant access confirmation")
        throw e
    }
    Try(client.sendEmail(
      accessGrantedBccTemplateId,
      teamEmail,
      personalisation,
      "access granted bcc")).recover {
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
