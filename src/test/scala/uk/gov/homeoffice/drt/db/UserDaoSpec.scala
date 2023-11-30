package uk.gov.homeoffice.drt.db

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import slick.jdbc.PostgresProfile.api._
import slick.lifted.TableQuery

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class UserDaoSpec extends Specification with BeforeEach {
  sequential

  private val numberOfInactivityDays = 60
  private val deactivateAfterWarningDays = 7
  val secondsInADay: Int = 24 * 60 * 60

  val userDao = UserDao(TestDatabase.db)

  val userActive1: User = User(
    id = "user1",
    username = "user1",
    email = "user1@test.com",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None,
    drop_in_notification_at = None,
    created_at = None
  )

  val userActive2: User = User(
    id = "user2",
    username = "user2",
    email = "user2@test.com",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None,
    drop_in_notification_at = None,
    created_at = None)

  val userInactiveMoreThan60days: User = User(
    id = "user3",
    username = "user3",
    email = "user3@test.com",
    latest_login = new Timestamp(Instant.now().minusSeconds(61 * secondsInADay).toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None,
    drop_in_notification_at = None,
    created_at = None)

  val userInactiveMoreThan67days: User = User(
    id = "user4",
    username = "user4",
    email = "user4@test.com",
    latest_login = new Timestamp(Instant.now().minusSeconds(68 * secondsInADay).toEpochMilli),
    inactive_email_sent = Some(new Timestamp(Instant.now().minusSeconds(8 * secondsInADay).toEpochMilli)),
    revoked_access = None,
    drop_in_notification_at = None,
    created_at = None)

  val userWithNoEmail: User = User(
    id = "user5",
    username = "user5",
    email = "",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None,
    drop_in_notification_at = None,
    created_at = None)

  def deleteUserTableData(db: Database, userTable: TableQuery[UserTable])(implicit executionContext: ExecutionContext): Int = {
    Await.result(db.run(userTable.delete), 1.seconds)
  }

  "select all" should "give all users" >> {
    val userList = List(userActive1, userActive2, userInactiveMoreThan60days, userInactiveMoreThan67days, userWithNoEmail)
    userDao.upsertUser(userActive1, Some("userTracking"))
    userDao.upsertUser(userActive2, Some("userTracking"))
    userDao.upsertUser(userInactiveMoreThan60days, Some("userTracking"))
    userDao.upsertUser(userInactiveMoreThan67days, Some("userTracking"))
    Await.result(userDao.upsertUser(userWithNoEmail, Some("userTracking")), 1.seconds)

    val allUser = Await.result(userDao.selectAll(), 1.seconds)
    allUser.toSet === userList.toSet
  }

  "select inactive user" should "give users who are inactive more than 60 days" >> {
    val expectedUsers = List(userInactiveMoreThan60days)
    userDao.upsertUser(userActive1, Some("userTracking"))
    userDao.upsertUser(userActive2, Some("userTracking"))
    userDao.upsertUser(userInactiveMoreThan60days, Some("userTracking"))
    userDao.upsertUser(userInactiveMoreThan67days, Some("userTracking"))
    Await.result(userDao.upsertUser(userWithNoEmail, Some("userTracking")), 1.seconds)

    val inactiveUsers = Await.result(userDao.selectInactiveUsers(numberOfInactivityDays), 1.seconds)
    inactiveUsers mustEqual expectedUsers
  }

  "select revoke access users" should "give users who are notified more that 7 days back about 60 days inactivity" >> {
    val expectedUsers = List(userInactiveMoreThan67days)
    userDao.upsertUser(userActive1,Some("userTracking"))
    userDao.upsertUser(userActive2,Some("userTracking"))
    userDao.upsertUser(userInactiveMoreThan60days,Some("userTracking"))
    userDao.upsertUser(userInactiveMoreThan67days,Some("userTracking"))
    Await.result(userDao.upsertUser(userWithNoEmail,Some("userTracking")), 1.seconds)

    val usersToRevokeAccess = Await.result(userDao.selectUsersToRevokeAccess(numberOfInactivityDays, deactivateAfterWarningDays), 1.seconds)

    usersToRevokeAccess mustEqual expectedUsers
  }

  "selected user" should "notified depending upon activity of user updated in user tracking" >> {
    //User activity
    userDao.upsertUser(userActive1.copy(latest_login = new Timestamp(Instant.now().minusSeconds(59 * secondsInADay).toEpochMilli)),Some("userTracking"))
    val noInactiveUser = Await.result(userDao.selectInactiveUsers(numberOfInactivityDays), 1.seconds)
    //No user activity in 61 days
    val inActiveUser = userActive1.copy(latest_login = new Timestamp(Instant.now().minusSeconds(61 * secondsInADay).toEpochMilli))
    userDao.upsertUser(inActiveUser,Some("userTracking"))
    val oneInActiveUser = Await.result(userDao.selectInactiveUsers(numberOfInactivityDays), 1.seconds)
    val noUserToRevoke = Await.result(userDao.selectUsersToRevokeAccess(numberOfInactivityDays, deactivateAfterWarningDays), 1.seconds)
    //No user activity in 68 days
    val revokedUser = inActiveUser.copy(latest_login = new Timestamp(Instant.now().minusSeconds(68 * secondsInADay).toEpochMilli),
      inactive_email_sent = Some(new Timestamp(Instant.now().minusSeconds(8 * secondsInADay).toEpochMilli)))
    userDao.upsertUser(revokedUser,Some("userTracking"))
    val oneUserToRevoke = Await.result(userDao.selectUsersToRevokeAccess(numberOfInactivityDays, deactivateAfterWarningDays), 1.seconds)

    oneInActiveUser.head mustEqual (inActiveUser)
    oneUserToRevoke.head mustEqual (revokedUser)
    noInactiveUser.isEmpty && noUserToRevoke.isEmpty
  }

  lazy val db: Database = Database.forConfig("h2-db")

  override protected def before: Any = {
    val schema = TestDatabase.userTable.schema
    Await.ready(db.run(DBIO.seq(schema.dropIfExists, schema.create)), 1.second)
  }

}
