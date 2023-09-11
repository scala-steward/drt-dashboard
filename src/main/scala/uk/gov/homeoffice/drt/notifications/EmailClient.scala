package uk.gov.homeoffice.drt.notifications

import org.slf4j.LoggerFactory
import uk.gov.service.notify.{NotificationClientApi, SendEmailResponse}

import scala.jdk.CollectionConverters.MapHasAsJava
import scala.util.Try

trait EmailClient {
  def send(templateId: String, emailAddress: String, personalisation: Map[String, Any]): Boolean
}

case class EmailClientImpl(govNotifyClient: NotificationClientApi) extends EmailClient {
  private val log = LoggerFactory.getLogger(getClass)

  def send(templateId: String, emailAddress: String, personalisation: Map[String, Any]): Boolean = {
    Try(govNotifyClient.sendEmail(templateId, emailAddress, personalisation.asJava, "")) match {
      case scala.util.Success(_: SendEmailResponse) => true
      case scala.util.Failure(t) =>
        log.error(s"Error while sending email to $emailAddress for template $templateId", t)
        false
    }
  }
}
