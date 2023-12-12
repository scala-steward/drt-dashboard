package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery
import spray.json._
import uk.gov.homeoffice.drt.ClientConfig
import uk.gov.homeoffice.drt.auth.Roles.BorderForceStaff
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
  with UserJsonSupport
  with BeforeEach {

  val testKit: ActorTestKit = ActorTestKit()
  implicit val sys: ActorSystem[Nothing] = testKit.system
  private val config: Config = ConfigFactory.load()
  val stringToLocalDateTime: String => Instant = dateString => Instant.parse(dateString)
  val clientConfig: ClientConfig = ClientConfig(Seq(PortRegion.North), Map(PortCode("NCL") -> Seq(T1)), "someDomain.com", "test@test.com")
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")
  val userDao: UserDao = UserDao(TestDatabase.db)
  val tableName = "user_route_test"

  val user1: User = User(
    "poise/test1",
    "poise/test1",
    "test1@test.com",
    new Timestamp(stringToLocalDateTime("2022-12-06T10:15:30.00Z").toEpochMilli),
    None,
    None,
    None,
    None)

  val user2: User = User(
    "poise/test2",
    "poise/test2",
    "test2@test.com",
    new Timestamp(stringToLocalDateTime("2022-12-05T10:15:30.00Z").toEpochMilli),
    None,
    None,
    None,
    None)

  def insertUser(userService: UserService): Future[Int] = {
    userService.upsertUser(user2,Some("userTracking"))
    userService.upsertUser(user1,Some("userTracking"))
  }

  def userRoutes(userService: UserService, userRequestService: UserRequestService): Route = UserRoutes(
    clientConfig,
    userService,
    userRequestService,
    EmailNotifications(List("access-requests@drt"), new MockNotificationClient),
    "")

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

    "Give list of all users accessing drt" >> {
      val userService = UserService(UserDao(TestDatabase.db))
      val userRequestService: UserRequestService = UserRequestService(new MockUserAccessRequestDao())
      Await.result(insertUser(userService), 5.seconds)
      Get("/users/all") ~>
        RawHeader("X-Auth-Roles", BorderForceStaff.name) ~>
        RawHeader("X-Auth-Email", "my@email.com") ~> userRoutes(userService, userRequestService) ~> check {
        val jsonUsers = responseAs[String].parseJson.asInstanceOf[JsArray].elements
        jsonUsers.contains(user1.toJson) && jsonUsers.contains(user2.toJson)
      }
    }

    "Saves user access request" >> {
      val userService = UserService(UserDao(TestDatabase.db))
      val userRequestService: UserRequestService = UserRequestService(new MockUserAccessRequestDao())
      Post("/users/access-request", accessRequest.toJson) ~>
        RawHeader("X-Auth-Roles", BorderForceStaff.name) ~>
        RawHeader("X-Auth-Email", "my@email.com") ~> userRoutes(userService, userRequestService) ~> check {
        responseAs[String] shouldEqual "OK"
      }
    }

    "Gives user access requested" >> {
      val userService = UserService(UserDao(TestDatabase.db))
      val userRequestService: UserRequestService = UserRequestService(new MockUserAccessRequestDao())
      val accessRequestToSave = accessRequest.copy(lineManager = "LineManager1")
      val currentTime = new Timestamp(DateTime.now().getMillis)
      userRequestService.saveUserRequest("my@email.com", accessRequestToSave, currentTime)
      Get("/users/access-request?status=\"Requested\"") ~>
        RawHeader("X-Auth-Roles", BorderForceStaff.name) ~>
        RawHeader("X-Auth-Email", "my@email.com") ~> userRoutes(userService, userRequestService) ~> check {
        responseAs[JsValue] shouldEqual Seq(expectedUserAccess(accessRequestToSave, currentTime)).toJson
      }
    }

  }

  def deleteUserTableData(db: Database, userTable: TableQuery[UserTable]): Int = {
    Await.result(db.run(userTable.delete), 5.seconds)
  }

  override protected def before: Any = {
    val schema = TestDatabase.userTable.schema
    Await.ready(TestDatabase.db.run(DBIO.seq(schema.dropIfExists, schema.create)), 1.second)
  }
}
