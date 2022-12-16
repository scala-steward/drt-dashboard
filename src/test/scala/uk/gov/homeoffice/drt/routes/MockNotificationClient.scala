package uk.gov.homeoffice.drt.routes

import uk.gov.service.notify.{ LetterResponse, Notification, NotificationClientApi, NotificationList, ReceivedTextMessageList, SendEmailResponse, SendLetterResponse, SendSmsResponse, Template, TemplateList, TemplatePreview }

import java.io.{ File, InputStream }
import java.util
import java.util.UUID

class MockNotificationClient extends NotificationClientApi {

  def response(
    notificationId: String = "",
    reference: String = "",
    templateId: String = "templateId",
    templateVersion: String = "2",
    templateUri: String = "uri",
    body: String = "body",
    subject: String = "subject",
    fromEmail: String = "") = {
    s"""{"id":"${UUID.randomUUID()}",
       | "notificationId":"$notificationId",
       | "reference":"$reference",
       | "template":{
       |    "id":"$templateId",
       |    "version":"$templateVersion",
       |    "uri":"$templateUri",
       | },
       | "content":{
       |  "body":"$body",
       |  "subject":"$subject",
       |  "fromEmail":"$fromEmail"
       | },
       |} """.stripMargin
  }

  override def sendEmail(templateId: String, emailAddress: String, personalisation: util.Map[String, _], reference: String): SendEmailResponse = {
    new SendEmailResponse(response(templateId = templateId, fromEmail = emailAddress, reference = reference))
  }

  override def sendEmail(templateId: String, emailAddress: String, personalisation: util.Map[String, _], reference: String, emailReplyToId: String): SendEmailResponse = ???

  override def sendSms(templateId: String, phoneNumber: String, personalisation: util.Map[String, _], reference: String): SendSmsResponse = ???

  override def sendSms(templateId: String, phoneNumber: String, personalisation: util.Map[String, _], reference: String, smsSenderId: String): SendSmsResponse = ???

  override def sendLetter(templateId: String, personalisation: util.Map[String, _], reference: String): SendLetterResponse = ???

  override def sendPrecompiledLetter(reference: String, precompiledPDF: File): LetterResponse = ???

  override def sendPrecompiledLetter(reference: String, precompiledPDF: File, postage: String): LetterResponse = ???

  override def sendPrecompiledLetterWithInputStream(reference: String, stream: InputStream): LetterResponse = ???

  override def sendPrecompiledLetterWithInputStream(reference: String, stream: InputStream, postage: String): LetterResponse = ???

  override def getNotificationById(notificationId: String): Notification = ???

  override def getPdfForLetter(notificationId: String): Array[Byte] = ???

  override def getNotifications(status: String, notification_type: String, reference: String, olderThanId: String): NotificationList = ???

  override def getTemplateById(templateId: String): Template = ???

  override def getTemplateVersion(templateId: String, version: Int): Template = ???

  override def getAllTemplates(templateType: String): TemplateList = ???

  override def generateTemplatePreview(templateId: String, personalisation: util.Map[String, AnyRef]): TemplatePreview = ???

  override def getReceivedTextMessages(olderThanId: String): ReceivedTextMessageList = ???
}
