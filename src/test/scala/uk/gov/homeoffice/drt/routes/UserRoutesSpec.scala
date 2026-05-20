package uk.gov.homeoffice.drt.routes

import org.apache.pekko.actor.testkit.typed.scaladsl.ActorTestKit
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.model.StatusCodes.OK
import org.apache.pekko.http.scaladsl.model.headers.RawHeader
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.RoutingLog
import org.apache.pekko.http.scaladsl.settings.{ParserSettings, RoutingSettings}
import org.apache.pekko.http.scaladsl.testkit.Specs2RouteTest
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import slick.jdbc.PostgresProfile.api._
import spray.json._
import uk.gov.homeoffice.drt.ClientConfig
import uk.gov.homeoffice.drt.auth.Roles.{BorderForceStaff, LHR}
import uk.gov.homeoffice.drt.authentication.{AccessRequest, AccessRequestJsonSupport, ClientUserAccessDataJsonSupport, ClientUserRequestedAccessData}
import uk.gov.homeoffice.drt.db._
import uk.gov.homeoffice.drt.keycloak.{IKeycloakService, KeyCloakUser}
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.services.{UserRequestService, UserService}

import java.sql.Timestamp
import java.time.Instant
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor, Future, Promise}

class UserRoutesSpec extends Specification
  with Specs2RouteTest
  with SprayJsonSupport
  with DefaultJsonProtocol
  with AccessRequestJsonSupport
  with ClientUserAccessDataJsonSupport
  with UserAccessRequestJsonSupport
  with UserRowJsonSupport
  with BeforeEach {

  sequential

  val testKit: ActorTestKit = ActorTestKit()
  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val routingSettings: RoutingSettings = RoutingSettings(system)
  implicit val parserSettings: ParserSettings = ParserSettings(system)
  implicit val routingLog: RoutingLog = RoutingLog(system.log)
  private val config: Config = ConfigFactory.load()
  val stringToLocalDateTime: String => Instant = dateString => Instant.parse(dateString)
  val clientConfig: ClientConfig = ClientConfig(Seq(PortRegion.North), () => Map(PortCode("NCL") -> Seq(T1)), "someDomain.com", "test@test.com")
  val apiKey: String = config.getString("dashboard.notifications.gov-notify-api-key")
  val userDao: UserDao = UserDao(TestDatabase)
  val tableName = "user_route_test"

  override protected def before: Any = {
    val userTable = TestDatabase.userTable.schema
    val accessRequestTable = TestDatabase.userAccessRequestsTable.schema
    Await.ready(TestDatabase.run(DBIO.seq(
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

  def userRoutes(userService: UserService,
                 userRequestService: UserRequestService,
                 keycloakServiceForToken: String => IKeycloakService,
                ): Route = UserRoutes(
    clientConfig,
    userService,
    userRequestService,
    EmailNotifications(List("access-requests@drt"), new MockNotificationClient),
    "",
    keycloakServiceForToken,
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

  def clientUserRequestedAccessData(timestamp: String): ClientUserRequestedAccessData =
    ClientUserRequestedAccessData(
      agreeDeclaration = accessRequest.agreeDeclaration,
      allPorts = accessRequest.allPorts,
      email = "my@email.com",
      lineManager = accessRequest.lineManager,
      portOrRegionText = accessRequest.portOrRegionText,
      portsRequested = accessRequest.portsRequested.mkString(","),
      accountType = accessRequest.rccOption,
      regionsRequested = accessRequest.regionsRequested.mkString(","),
      requestTime = timestamp,
      staffText = accessRequest.staffText,
      staffEditing = accessRequest.staffing,
      status = "Requested",
    )

  def accessRequestFor(allPorts: Boolean = false,
                       portsRequested: Set[String] = Set("lhr"),
                       accountType: String = "",
                       regionsRequested: Set[String] = Set.empty,
                       staffEditing: Boolean = false,
                      ): AccessRequest =
    AccessRequest(
      agreeDeclaration = false,
      allPorts = allPorts,
      lineManager = "lineManager",
      portOrRegionText = "Need dashboard access",
      portsRequested = portsRequested,
      rccOption = accountType,
      regionsRequested = regionsRequested,
      staffing = staffEditing,
      staffText = if (staffEditing) "Needs staff editing" else "",
    )

  def clientUserRequestedAccessDataFor(request: AccessRequest,
                                       timestamp: String,
                                       email: String = "my@email.com",
                                      ): ClientUserRequestedAccessData =
    ClientUserRequestedAccessData(
      agreeDeclaration = request.agreeDeclaration,
      allPorts = request.allPorts,
      email = email,
      lineManager = request.lineManager,
      portOrRegionText = request.portOrRegionText,
      portsRequested = request.portsRequested.mkString(","),
      accountType = request.rccOption,
      regionsRequested = request.regionsRequested.mkString(","),
      requestTime = timestamp,
      staffText = request.staffText,
      staffEditing = request.staffing,
      status = "Requested",
    )

  class StubKeycloakService(addUserToGroupResponse: String => Future[HttpResponse]) extends IKeycloakService {
    override implicit val ec: ExecutionContextExecutor = sys.executionContext
    val addedGroups: ListBuffer[(String, String)] = ListBuffer.empty

    override def getUserForEmail(email: String): Future[Option[KeyCloakUser]] = Future.successful(None)

    override def removeUser(userId: String): Future[HttpResponse] = Future.successful(HttpResponse(StatusCodes.OK))

    override def addUserToGroup(userId: String, group: String): Future[HttpResponse] = {
      addedGroups.append((userId, group))
      addUserToGroupResponse(group)
    }

    override def logout(username: String): Future[Option[Future[HttpResponse]]] = Future.successful(None)
  }

  "User api" >> {
    val userService = UserService(UserDao(TestDatabase))
    val userRequestService: UserRequestService = UserRequestService(UserAccessRequestDao(TestDatabase))
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

    "wait for all Keycloak group updates before completing access approval" >> {
      val currentTimeString = "2026-01-01 10:15:30.000"
      val currentTime = Timestamp.valueOf("2026-01-01 10:15:30")
      val pendingBorderForce = Promise[HttpResponse]()
      val keycloakService = new StubKeycloakService(group =>
        if (group == "Border Force") pendingBorderForce.future else Future.successful(HttpResponse(StatusCodes.OK)))
      val routes = userRoutes(userService, userRequestService, _ => keycloakService)

      Await.ready(userRequestService.saveUserRequest("my@email.com", accessRequest, currentTime), 1.second)

      val handler = Route.asyncHandler(routes)
      val responseFuture = handler(
        Post("/users/accept-access-request/keycloak-user-id", clientUserRequestedAccessData(currentTimeString).toJson)
          .withHeaders(
            RawHeader("X-Forwarded-Groups", "manage-users"),
            RawHeader("X-Forwarded-Email", "manager@email.com"),
            RawHeader("X-Forwarded-Access-Token", "token"),
          ),
      )

      responseFuture.isCompleted mustEqual false

      pendingBorderForce.success(HttpResponse(StatusCodes.OK))

      val response = Await.result(responseFuture, 1.second)
      response.status shouldEqual OK
      keycloakService.addedGroups.map(_._2).toSet shouldEqual Set("lhr", "Border Force")
    }

    "not mark an access request as approved when any Keycloak group update fails" >> {
      val currentTimeString = "2026-01-01 10:15:30.000"
      val currentTime = Timestamp.valueOf("2026-01-01 10:15:30")
      val keycloakService = new StubKeycloakService(group =>
        if (group == "Border Force") Future.failed(new Exception("Keycloak rejected Border Force group assignment"))
        else Future.successful(HttpResponse(StatusCodes.OK)))
      val routes = userRoutes(userService, userRequestService, _ => keycloakService)

      Await.ready(userRequestService.saveUserRequest("my@email.com", accessRequest, currentTime), 1.second)

      Post("/users/accept-access-request/keycloak-user-id", clientUserRequestedAccessData(currentTimeString).toJson) ~>
        RawHeader("X-Forwarded-Groups", "manage-users") ~>
        RawHeader("X-Forwarded-Email", "manager@email.com") ~>
        RawHeader("X-Forwarded-Access-Token", "token") ~> routes ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] must contain("Keycloak rejected Border Force group assignment")
      }

      Await.result(userRequestService.getUserRequest("Approved"), 1.second) shouldEqual Seq.empty
      Await.result(userRequestService.getUserRequest("Requested"), 1.second) should haveSize(1)
      Await.result(userService.getUsers(), 1.second).map(_.email) should not contain("my@email.com")
    }

    "grant all port access and border force groups for all-port approvals" >> {
      val currentTimeString = "2026-01-02 10:15:30.000"
      val currentTime = Timestamp.valueOf("2026-01-02 10:15:30")
      val request = accessRequestFor(allPorts = true, portsRequested = Set.empty)
      val clientData = clientUserRequestedAccessDataFor(request, currentTimeString)
      val keycloakService = new StubKeycloakService(_ => Future.successful(HttpResponse(StatusCodes.OK)))
      val routes = userRoutes(userService, userRequestService, _ => keycloakService)

      Await.ready(userRequestService.saveUserRequest("my@email.com", request, currentTime), 1.second)

      Post("/users/accept-access-request/keycloak-user-id", clientData.toJson) ~>
        RawHeader("X-Forwarded-Groups", "manage-users") ~>
        RawHeader("X-Forwarded-Email", "manager@email.com") ~>
        RawHeader("X-Forwarded-Access-Token", "token") ~> routes ~> check {
        status shouldEqual OK
        keycloakService.addedGroups.map(_._2).toSet shouldEqual Set("All Port Access", "Border Force")
      }
    }

    "grant all RCC access when approving an RCCU all-port request" >> {
      val currentTimeString = "2026-01-03 10:15:30.000"
      val currentTime = Timestamp.valueOf("2026-01-03 10:15:30")
      val request = accessRequestFor(allPorts = true, portsRequested = Set.empty, accountType = "rccu")
      val clientData = clientUserRequestedAccessDataFor(request, currentTimeString)
      val keycloakService = new StubKeycloakService(_ => Future.successful(HttpResponse(StatusCodes.OK)))
      val routes = userRoutes(userService, userRequestService, _ => keycloakService)

      Await.ready(userRequestService.saveUserRequest("my@email.com", request, currentTime), 1.second)

      Post("/users/accept-access-request/keycloak-user-id", clientData.toJson) ~>
        RawHeader("X-Forwarded-Groups", "manage-users") ~>
        RawHeader("X-Forwarded-Email", "manager@email.com") ~>
        RawHeader("X-Forwarded-Access-Token", "token") ~> routes ~> check {
        status shouldEqual OK
        keycloakService.addedGroups.map(_._2).toSet shouldEqual Set("All Port Access", "All RCC Access", "Border Force")
      }
    }

    "grant staff admin when approving a staffing-edit request" >> {
      val currentTimeString = "2026-01-04 10:15:30.000"
      val currentTime = Timestamp.valueOf("2026-01-04 10:15:30")
      val request = accessRequestFor(staffEditing = true)
      val clientData = clientUserRequestedAccessDataFor(request, currentTimeString)
      val keycloakService = new StubKeycloakService(_ => Future.successful(HttpResponse(StatusCodes.OK)))
      val routes = userRoutes(userService, userRequestService, _ => keycloakService)

      Await.ready(userRequestService.saveUserRequest("my@email.com", request, currentTime), 1.second)

      Post("/users/accept-access-request/keycloak-user-id", clientData.toJson) ~>
        RawHeader("X-Forwarded-Groups", "manage-users") ~>
        RawHeader("X-Forwarded-Email", "manager@email.com") ~>
        RawHeader("X-Forwarded-Access-Token", "token") ~> routes ~> check {
        status shouldEqual OK
        keycloakService.addedGroups.map(_._2).toSet shouldEqual Set("lhr", "Border Force", "Staff Admin")
      }
    }

    "wait for all requested groups when approving access for multiple ports" >> {
      val currentTimeString = "2026-01-05 10:15:30.000"
      val currentTime = Timestamp.valueOf("2026-01-05 10:15:30")
      val request = accessRequestFor(portsRequested = Set("lhr", "man"), staffEditing = true)
      val clientData = clientUserRequestedAccessDataFor(request, currentTimeString)
      val delayedGroup = Promise[HttpResponse]()
      val keycloakService = new StubKeycloakService(group =>
        if (group == "man") delayedGroup.future else Future.successful(HttpResponse(StatusCodes.OK)))
      val routes = userRoutes(userService, userRequestService, _ => keycloakService)

      Await.ready(userRequestService.saveUserRequest("my@email.com", request, currentTime), 1.second)

      val handler = Route.asyncHandler(routes)
      val responseFuture = handler(
        Post("/users/accept-access-request/keycloak-user-id", clientData.toJson)
          .withHeaders(
            RawHeader("X-Forwarded-Groups", "manage-users"),
            RawHeader("X-Forwarded-Email", "manager@email.com"),
            RawHeader("X-Forwarded-Access-Token", "token"),
          ),
      )

      responseFuture.isCompleted mustEqual false

      delayedGroup.success(HttpResponse(StatusCodes.OK))

      val response = Await.result(responseFuture, 1.second)
      response.status shouldEqual OK
      keycloakService.addedGroups.map(_._2).toSet shouldEqual Set("lhr", "man", "Border Force", "Staff Admin")
    }

    "fail the approval when one of multiple requested group assignments fails" >> {
      val currentTimeString = "2026-01-06 10:15:30.000"
      val currentTime = Timestamp.valueOf("2026-01-06 10:15:30")
      val request = accessRequestFor(portsRequested = Set("lhr", "man"), staffEditing = true)
      val clientData = clientUserRequestedAccessDataFor(request, currentTimeString)
      val keycloakService = new StubKeycloakService(group =>
        if (group == "man") Future.failed(new Exception("Keycloak rejected MAN group assignment"))
        else Future.successful(HttpResponse(StatusCodes.OK)))
      val routes = userRoutes(userService, userRequestService, _ => keycloakService)

      Await.ready(userRequestService.saveUserRequest("my@email.com", request, currentTime), 1.second)

      Post("/users/accept-access-request/keycloak-user-id", clientData.toJson) ~>
        RawHeader("X-Forwarded-Groups", "manage-users") ~>
        RawHeader("X-Forwarded-Email", "manager@email.com") ~>
        RawHeader("X-Forwarded-Access-Token", "token") ~> routes ~> check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] must contain("Keycloak rejected MAN group assignment")
      }

      Await.result(userRequestService.getUserRequest("Approved"), 1.second) shouldEqual Seq.empty
      Await.result(userRequestService.getUserRequest("Requested"), 1.second) should haveSize(1)
      Await.result(userService.getUsers(), 1.second).map(_.email) should not contain("my@email.com")
    }
  }
}
