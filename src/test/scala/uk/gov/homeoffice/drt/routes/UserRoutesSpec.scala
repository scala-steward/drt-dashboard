package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import slick.jdbc.PostgresProfile.api._
import spray.json._
import uk.gov.homeoffice.drt.ClientConfig
import uk.gov.homeoffice.drt.auth.Roles.{BorderForceStaff, LHR}
import uk.gov.homeoffice.drt.authentication.{AccessRequest, AccessRequestJsonSupport}
import uk.gov.homeoffice.drt.db._
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.services.{UserRequestService, UserService}

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class UserRoutesSpec extends Specification
  with Specs2RouteTest
  with SprayJsonSupport
  with DefaultJsonProtocol
  with AccessRequestJsonSupport
  with UserAccessRequestJsonSupport
  with UserRowJsonSupport
  with BeforeEach {

  sequential

  val testKit: ActorTestKit = ActorTestKit()
  implicit val sys: ActorSystem[Nothing] = testKit.system
  private val config: Config = ConfigFactory.load()
  val stringToLocalDateTime: String => Instant = dateString => Instant.parse(dateString)
  val clientConfig: ClientConfig = ClientConfig(Seq(PortRegion.North), Map(PortCode("NCL") -> Seq(T1)), "someDomain.com", "test@test.com")
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")
  val userDao: UserDao = UserDao(TestDatabase.db)
  val tableName = "user_route_test"

  override protected def before: Any = {
    val userTable = TestDatabase.userTable.schema
    val accessRequestTable = TestDatabase.userAccessRequestsTable.schema
    Await.ready(TestDatabase.db.run(DBIO.seq(
      userTable.dropIfExists,
      userTable.create,
      accessRequestTable.dropIfExists,
      accessRequestTable.create,
    )), 2.second)
  }

  val user1: UserRow = UserRow(
    "poise/test1",
    "poise/test1",
    "test1@test.com",
    new Timestamp(stringToLocalDateTime("2022-12-06T10:15:30.00Z").toEpochMilli),
    None,
    None,
    None,
    None)

  val user2: UserRow = UserRow(
    "poise/test2",
    "poise/test2",
    "test2@test.com",
    new Timestamp(stringToLocalDateTime("2022-12-05T10:15:30.00Z").toEpochMilli),
    None,
    None,
    None,
    None)

  def insertUser(userService: UserService): Future[Int] = {
    userService.upsertUser(user2, Some("userTracking"))
    userService.upsertUser(user1, Some("userTracking"))
  }

  def userRoutes(userService: UserService, userRequestService: UserRequestService): Route = UserRoutes(
    clientConfig,
    userService,
    userRequestService,
    EmailNotifications(List("access-requests@drt"), new MockNotificationClient),
    "",
  )

  val accessRequest: AccessRequest = AccessRequest(
    agreeDeclaration = false,
    allPorts = false,
    "lineManager",
    "lhr",
    Set("lhr"),
    "",
    Set.empty,
    staffing = false,
    "")

  def expectedUserAccess(accessRequest: AccessRequest, timestamp: Timestamp): UserAccessRequest = {
    UserAccessRequest(
      email = "my@email.com",
      portsRequested = accessRequest.portsRequested.mkString(","),
      allPorts = accessRequest.allPorts,
      regionsRequested = accessRequest.regionsRequested.mkString(","),
      staffEditing = accessRequest.staffing,
      lineManager = accessRequest.lineManager,
      agreeDeclaration = accessRequest.agreeDeclaration,
      accountType = accessRequest.rccOption,
      portOrRegionText = accessRequest.portOrRegionText,
      staffText = accessRequest.staffText,
      status = "Requested",
      requestTime = timestamp)
  }

  "User api" >> {
    val userService = UserService(UserDao(TestDatabase.db))
    val userRequestService: UserRequestService = UserRequestService(UserAccessRequestDao(TestDatabase.db))
    val routes = userRoutes(userService, userRequestService)

    "Given a uri accessed by a user with an email but no port access, I should see an empty port list and their email address in JSON" >> {
      Get("/user") ~>
        RawHeader("X-Forwarded-Groups", BorderForceStaff.name) ~>
        RawHeader("X-Forwarded-Email", "my1@email.com") ~> routes ~> check {
        responseAs[String] shouldEqual """{"ports":[],"roles":["border-force-staff"],"email":"my1@email.com"}"""
      }
    }

    "Given a uri accessed by a user with an email but no port access, I should see an empty port list and their email address in JSON" >> {
      Get("/user") ~>
        RawHeader("X-Forwarded-Groups", BorderForceStaff.name) ~>
        RawHeader("X-Forwarded-Email", "my1@email.com") ~> routes ~> check {
        responseAs[String] shouldEqual """{"ports":[],"roles":["border-force-staff"],"email":"my1@email.com"}"""
      }
    }

    "Given a uri accessed by a user with an email and LHR port access, I should see LHR in the port list and their email address in JSON" >> {
      Get("/user") ~>
        RawHeader("X-Forwarded-Groups", Seq(BorderForceStaff.name, LHR.name).mkString(",")) ~>
        RawHeader("X-Forwarded-Email", "my1@email.com") ~> routes ~> check {
        responseAs[String] shouldEqual """{"ports":["LHR"],"roles":["border-force-staff","LHR"],"email":"my1@email.com"}"""
      }
    }

    "When user tracking is received with expected headers, user details is present in user table" >> {
      Get("/track-user") ~>
        RawHeader("X-Forwarded-Groups", "") ~>
        RawHeader("X-Forwarded-Preferred-Username", "my1") ~>
        RawHeader("X-Forwarded-Email", "my1@email.com") ~> routes ~> check {
        responseAs.status shouldEqual OK
        val users = Await.result(userService.getUsers(), 1.seconds)
        users.exists(u => u.id == "my1" && u.username == "my1" && u.email == "my1@email.com" && u.latest_login.getTime - System.currentTimeMillis() < 100)
      }
    }

    "Give list of all users accessing drt" >> {
      Await.result(insertUser(userService), 5.seconds)
      Get("/users/all") ~>
        RawHeader("X-Forwarded-Groups", s"role:${BorderForceStaff.name}") ~>
        RawHeader("X-Forwarded-Email", "my@email.com") ~> routes ~> check {
        val jsonUsers = responseAs[String].parseJson.asInstanceOf[JsArray].elements
        jsonUsers.contains(user1.toJson) && jsonUsers.contains(user2.toJson)
      }
    }

    "Saves user access request" >> {
      Post("/users/access-request", accessRequest.toJson) ~>
        RawHeader("X-Forwarded-Groups", s"role:${BorderForceStaff.name}") ~>
        RawHeader("X-Forwarded-Email", "my@email.com") ~> routes ~> check {
        responseAs[String] shouldEqual "OK"
      }
    }

    "Gives user access requested" >> {
      val accessRequestToSave = accessRequest.copy(lineManager = "LineManager1")
      val currentTime = new Timestamp(DateTime.now().getMillis)

      Await.ready(userRequestService.saveUserRequest("my@email.com", accessRequestToSave, currentTime), 1.second)

      Get("/users/access-request?status=Requested") ~>
        RawHeader("X-Forwarded-Groups", s"role:${BorderForceStaff.name}") ~>
        RawHeader("X-Forwarded-Email", "my@email.com") ~> routes ~> check {
        responseAs[JsValue] shouldEqual Seq(expectedUserAccess(accessRequestToSave, currentTime)).toJson
      }
    }
  }
}
