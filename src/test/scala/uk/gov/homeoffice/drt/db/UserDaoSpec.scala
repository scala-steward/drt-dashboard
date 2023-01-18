package uk.gov.homeoffice.drt.db

import org.specs2.mutable.Specification
import org.specs2.specification.{ AfterEach, BeforeEach }
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class UserDaoSpec extends Specification with AfterEach with BeforeEach {

  val secondsInADay = 24 * 60 * 60
  var appDatabaseTest: AppTestDatabase = null

  val userActive1 = User(
    id = "user1",
    username = "user1",
    email = "user1@test.com",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None)

  val userActive2 = User(
    id = "user2",
    username = "user2",
    email = "user2@test.com",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None)

  val userInactiveMoreThan60days = User(
    id = "user3",
    username = "user3",
    email = "user3@test.com",
    latest_login = new Timestamp(Instant.now().minusSeconds(61 * secondsInADay).toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None)

  val userInactiveMoreThan67days = User(
    id = "user4",
    username = "user4",
    email = "user4@test.com",
    latest_login = new Timestamp(Instant.now().minusSeconds(61 * secondsInADay).toEpochMilli),
    inactive_email_sent = Some(new Timestamp(Instant.now().minusSeconds(8 * secondsInADay).toEpochMilli)),
    revoked_access = None)

  val userWithNoEmail = User(
    id = "user5",
    username = "user5",
    email = "",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None)

  def deleteUserTableData(db: Database, userTable: TableQuery[UserTable])(implicit executionContext: ExecutionContext): Int = {
    Await.result(db.run(userTable.delete), 1.seconds)
  }

  "select all" should "give all users" >> {
    val userList = List(userActive1, userActive2, userInactiveMoreThan60days, userInactiveMoreThan67days, userWithNoEmail)
    val appDatabaseTest = new AppTestDatabase()
    val userDao = new UserDao(appDatabaseTest.db, appDatabaseTest.userTestTable)
    userDao.insertOrUpdate(userActive1)
    userDao.insertOrUpdate(userActive2)
    userDao.insertOrUpdate(userInactiveMoreThan60days)
    userDao.insertOrUpdate(userInactiveMoreThan67days)
    Await.result(userDao.insertOrUpdate(userWithNoEmail), 1.seconds)

    val allUser = Await.result(userDao.selectAll(), 1.seconds)
    allUser.toSet === userList.toSet
  }

  "select inactive user" should "give users who are inactive more than 60 days" >> {
    val expectedUsers = List(userInactiveMoreThan60days)
    val appDatabaseTest = new AppTestDatabase()
    val userDao = new UserDao(appDatabaseTest.db, appDatabaseTest.userTestTable)
    userDao.insertOrUpdate(userActive1)
    userDao.insertOrUpdate(userActive2)
    userDao.insertOrUpdate(userInactiveMoreThan60days)
    userDao.insertOrUpdate(userInactiveMoreThan67days)
    Await.result(userDao.insertOrUpdate(userWithNoEmail), 1.seconds)

    val inactiveUsers = Await.result(userDao.selectInactiveUsers(60), 1.seconds)
    inactiveUsers mustEqual expectedUsers
  }

  "select revoke access users" should "give users who are notified more that 7 days back about 60 days inactivity" >> {
    val expectedUsers = List(userInactiveMoreThan67days)
    val userDao = new UserDao(appDatabaseTest.db, appDatabaseTest.userTestTable)
    userDao.insertOrUpdate(userActive1)
    userDao.insertOrUpdate(userActive2)
    userDao.insertOrUpdate(userInactiveMoreThan60days)
    userDao.insertOrUpdate(userInactiveMoreThan67days)
    Await.result(userDao.insertOrUpdate(userWithNoEmail), 1.seconds)

    val usersToRevokeAccess = Await.result(userDao.selectUsersToRevokeAccess, 1.seconds)

    usersToRevokeAccess mustEqual expectedUsers
  }

  override protected def after: Any = {
    appDatabaseTest.db.close()
  }

  override protected def before: Any = {
    appDatabaseTest = new AppTestDatabase()
    deleteUserTableData(appDatabaseTest.db, appDatabaseTest.userTestTable)
  }

}
