package uk.gov.homeoffice.drt.services

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito._
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.BeforeEach
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.authentication.{AccessRequest, ClientUserRequestedAccessData}
import uk.gov.homeoffice.drt.db._
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.service.notify.{NotificationClientApi, SendEmailResponse}

import java.sql.Timestamp
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class DropInServiceSpec extends SpecificationLike with BeforeEach {

  sequential

  implicit val sys: ActorSystem = ActorSystem("testActorSystem", ConfigFactory.empty())

  val teamEmail = "test@test.com"
  val rootDomain = "localhost"

  override protected def before: Any = {
    Await.ready(
      TestDatabase.run(DBIO.seq(
        TestDatabase.userTable.schema.dropIfExists,
        TestDatabase.userTable.schema.createIfNotExists,
        TestDatabase.userAccessRequestsTable.schema.dropIfExists,
        TestDatabase.userAccessRequestsTable.schema.createIfNotExists,
        TestDatabase.dropInTable.schema.dropIfExists,
        TestDatabase.dropInTable.schema.createIfNotExists,
        TestDatabase.dropInRegistrationTable.schema.dropIfExists,
        TestDatabase.dropInRegistrationTable.schema.createIfNotExists)
      ), 2.second)
  }

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

  def getUser(createdAt: String) = {
    UserRow(id = "test",
      username = "test",
      email = "test@test.com",
      latest_login = new Timestamp(1693609200000L),
      inactive_email_sent = None,
      revoked_access = None,
      drop_in_notification_at = None,
      created_at = Some(new Timestamp(DateTime.parse(createdAt, DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS")).getMillis)))
  }

  def getAccessRequest = {
    AccessRequest(agreeDeclaration = true,
      allPorts = false,
      lineManager = "lineManager",
      portOrRegionText = "port",
      portsRequested = Set("lhr"),
      rccOption = "",
      regionsRequested = Set(),
      staffing = false,
      staffText = "")
  }

  def clientUserRequestedAccessData(accessRequest: AccessRequest, requestTime: String) = {
    ClientUserRequestedAccessData(agreeDeclaration = true,
      allPorts = false,
      email = "test@test.com",
      lineManager = accessRequest.lineManager,
      portOrRegionText = accessRequest.portOrRegionText,
      portsRequested = accessRequest.portsRequested.mkString(","),
      accountType = accessRequest.portOrRegionText,
      regionsRequested = accessRequest.regionsRequested.mkString(","),
      requestTime = requestTime,
      staffText = accessRequest.staffText,
      staffEditing = accessRequest.staffing,
      status = "Requested")
  }

  def firstSeptember2023 = {
    new Timestamp(DateTime.parse("2023-09-01 00:00:00.000", DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS")).getMillis)
  }

  def runScenario(createdAt: String, registeredForDropIn: Boolean, resendCheck: Boolean) = {
    val dropInDao = DropInDao(TestDatabase)
    val dropInRegistrationDao = DropInRegistrationDao(TestDatabase)
    val userService = UserService(UserDao(TestDatabase))
    val userRequestService = UserRequestService(UserAccessRequestDao(TestDatabase))
    val dropInService: DropInService = new DropInService(dropInDao,
      dropInRegistrationDao,
      userService,
      userRequestService, teamEmail)
    val emailClient = Mockito.mock(classOf[NotificationClientApi])
    when(emailClient.sendEmail(any(), any(), any(), any())).thenReturn(new SendEmailResponse(response(templateId = UUID.randomUUID().toString)))
    val emailNotifications = EmailNotifications(List("test@test.com"), emailClient)

    val isCreatedAtBefore = DateTime.parse(createdAt, DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss.SSS")).isBefore(new DateTime(firstSeptember2023.getTime))
    val accessRequest = getAccessRequest
    userRequestService.updateUserRequest(
      clientUserRequestedAccessData(accessRequest, createdAt), "Approved")

    Await.result(userService.upsertUser(getUser(createdAt), Some("Approved")), 1.second)

    def sendNotificationAndUserExistCheck() = {
      val beforeDropInNotification = Await.result(userService.getUsers(), 1.second)
      beforeDropInNotification.head.drop_in_notification_at.isDefined === false
      beforeDropInNotification.size === 1

      Await.result(dropInService.sendDropInNotificationToNewUsers(emailNotifications, "localhost"), 1.second)

    }

    def sendNotificationAndUserDropInNotificationIsEmptyCheck() = {
      val beforeDropInNotification = Await.result(userService.getUsersWithoutDropInNotification, 1.second)
      beforeDropInNotification.head.drop_in_notification_at.isDefined === false
      beforeDropInNotification.size === 1

      Await.result(dropInService.sendDropInNotificationToNewUsers(emailNotifications, "localhost"), 1.second)

    }

    (isCreatedAtBefore, registeredForDropIn, resendCheck) match {
      case (true, _, _) =>
        sendNotificationAndUserExistCheck()
        //Notification should not be sent when user is created before 1st September 2023
        Mockito.verify(emailClient, Mockito.times(0)).sendEmail(any, any(), any(), any())

        val afterDropInNotification: Seq[UserRow] = Await.result(userService.getUsers(), 1.second)
        afterDropInNotification.head.drop_in_notification_at.isDefined === false
        afterDropInNotification.size === 1

      case (false, _, _) =>
        sendNotificationAndUserDropInNotificationIsEmptyCheck()
        //Notification should be sent when user is created after 1st September 2023
        Mockito.verify(emailClient, Mockito.times(1)).sendEmail(any, any(), any(), any())
        val afterDropInNotification: Seq[UserRow] = Await.result(userService.getUsers(), 1.second)
        afterDropInNotification.head.drop_in_notification_at.isDefined === true
        afterDropInNotification.size === 1

      case (false, true, _) =>
        dropInDao.insertDropIn("test",
          new Timestamp(DateTime.now().minusSeconds(60).getMillis),
          new Timestamp(DateTime.now().minusSeconds(30).getMillis),
          None)

        dropInRegistrationDao.insertRegistration("test@test.com", 1, new Timestamp(DateTime.now().minusSeconds(30).getMillis), None)
        sendNotificationAndUserDropInNotificationIsEmptyCheck()
        // Notification is not sent as user is already registered for drop in
        Mockito.verify(emailClient, Mockito.times(0)).sendEmail(any, any(), any(), any())

        val afterDropInNotification: Seq[UserRow] = Await.result(userService.getUsers(), 1.second)
        afterDropInNotification.head.drop_in_notification_at.isDefined === false
        afterDropInNotification.size === 1

      case (false, false, true) =>
        sendNotificationAndUserDropInNotificationIsEmptyCheck()
        Mockito.verify(emailClient, Mockito.times(1)).sendEmail(any, any(), any(), any())
        val afterDropInNotification: Seq[UserRow] = Await.result(userService.getUsers(), 1.second)
        afterDropInNotification.head.drop_in_notification_at.isDefined === true
        afterDropInNotification.size === 1
        Await.result(dropInService.sendDropInNotificationToNewUsers(emailNotifications, rootDomain), 1.second)
        //After resending the DropIn notification and sendEmail is not called again which mean once notification is sent it will not be sent again
        Mockito.verify(emailClient, Mockito.times(1)).sendEmail(any, any(), any(), any())
        val replayDropInNotification: Seq[UserRow] = Await.result(userService.getUsers(), 1.second)
        replayDropInNotification.head.drop_in_notification_at.isDefined === true
        replayDropInNotification.size === 1
        replayDropInNotification.head.drop_in_notification_at === afterDropInNotification.head.drop_in_notification_at
    }


  }

  "DropInService" >> {
    "Send dropIn notification if user is requested access after 1 September" >> {
      runScenario(createdAt = "2023-09-02 15:10:10.000", registeredForDropIn = false, resendCheck = false)
    }

    "Don't Send dropIn notification if user is requested access before 1 September" >> {
      runScenario(createdAt = "2023-08-30 15:10:10.000", registeredForDropIn = false, resendCheck = false)
    }

    "Don't Send dropIn notification if user is has being registered for dropIn" >> {
      runScenario(createdAt = "2023-09-02 15:10:10.000", registeredForDropIn = true, resendCheck = false)
    }

    "Don't Send dropIn notification if user is has being send notification already" >> {
      runScenario(createdAt = "2023-09-02 15:10:10.000", registeredForDropIn = false, resendCheck = true)
    }
  }
}
