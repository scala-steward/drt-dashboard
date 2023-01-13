package uk.gov.homeoffice.drt.db

import org.scalatest.flatspec.AsyncFlatSpec

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class MockUserDaoSpec extends AsyncFlatSpec {
  implicit override def executionContext = scala.concurrent.ExecutionContext.Implicits.global

  val secondsInADay = 24 * 60 * 60
  val user1 = User(
    id = "user1@test.com",
    username = "user1",
    email = "user1@test.com",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None)

  val user2 = User(
    id = "user2@test.com",
    username = "user2",
    email = "user2@test.com",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None)

  val user3 = User(
    id = "user3@test.com",
    username = "user3",
    email = "user3@test.com",
    latest_login = new Timestamp(Instant.now().minusSeconds(61 * secondsInADay).toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None)

  val user4 = User(
    id = "user4@test.com",
    username = "user4",
    email = "user4@test.com",
    latest_login = new Timestamp(Instant.now().minusSeconds(61 * secondsInADay).toEpochMilli),
    inactive_email_sent = Some(new Timestamp(Instant.now().minusSeconds(8 * secondsInADay).toEpochMilli)),
    revoked_access = None)

  val user5 = User(
    id = "user5@test.com",
    username = "user5",
    email = "",
    latest_login = new Timestamp(Instant.now().toEpochMilli),
    inactive_email_sent = None,
    revoked_access = None)

  "UserList" should "give all users" in {
    val userList = List(user1, user2, user3, user4, user5)
    val mockUserDao = new MockUserDao()
    mockUserDao.insertOrUpdate(user1)
    mockUserDao.insertOrUpdate(user2)
    mockUserDao.insertOrUpdate(user3)
    mockUserDao.insertOrUpdate(user4)
    mockUserDao.insertOrUpdate(user5)

    val allUser = Await.result(mockUserDao.selectAll(), 1.seconds)

    assert(allUser == userList)
  }

  "UserList" should "give users who are inactive more than 60 days" in {
    val expectedUsers = List(user3)
    val mockUserDao = new MockUserDao()
    mockUserDao.insertOrUpdate(user1)
    mockUserDao.insertOrUpdate(user2)
    mockUserDao.insertOrUpdate(user3)
    mockUserDao.insertOrUpdate(user4)
    mockUserDao.insertOrUpdate(user5)

    val inactiveUsers = Await.result(mockUserDao.selectInactiveUsers(60), 5.seconds)

    assert(inactiveUsers == expectedUsers)
  }

  "UserList" should "give users who are notified more that 7 days back" in {
    val expectedUsers = List(user4)
    val mockUserDao = new MockUserDao()
    mockUserDao.insertOrUpdate(user1)
    mockUserDao.insertOrUpdate(user2)
    mockUserDao.insertOrUpdate(user3)
    mockUserDao.insertOrUpdate(user4)
    mockUserDao.insertOrUpdate(user5)

    val usersToRevokeAccess = Await.result(mockUserDao.selectUsersToRevokeAccess, 5.seconds)

    assert(usersToRevokeAccess == expectedUsers)
  }
}
