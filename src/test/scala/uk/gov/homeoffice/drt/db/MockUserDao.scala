package uk.gov.homeoffice.drt.db

import scala.concurrent.{ExecutionContext, Future}
import java.sql.Timestamp
import java.time.Instant

class MockUserDao extends IUserDao {
  var userList = Seq.empty[User]
  val secondsInADay = 60 * 60 * 24

  def upsertUser(user: User, purpose: Option[String])(implicit ec: ExecutionContext): Future[Int] = {
    userList = userList :+ user
    Future.successful(userList.size)
  }

  override def selectInactiveUsers(numberOfInactivityDays: Int)
    (implicit executionContext: ExecutionContext): Future[Seq[User]] =
    Future.successful(userList.filter(user => user.inactive_email_sent.isEmpty &&
      user.latest_login.before(new Timestamp(Instant.now().minusSeconds(numberOfInactivityDays * secondsInADay).toEpochMilli))))

  override def selectUsersToRevokeAccess(numberOfInactivityDays: Int, deactivateAfterWarningDays: Int)
    (implicit executionContext: ExecutionContext): Future[Seq[User]] =
    Future.successful(userList.filter(user => user.revoked_access.isEmpty &&
      user.latest_login.before(new Timestamp(Instant.now().minusSeconds((numberOfInactivityDays + deactivateAfterWarningDays) * secondsInADay).toEpochMilli)) &&
      user.inactive_email_sent.exists(_.before(new Timestamp(Instant.now().minusSeconds((deactivateAfterWarningDays) * secondsInADay).toEpochMilli)))))

  override def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[User]] = {
    Future.successful(userList)
  }

  override def getUsersWithoutDropInNotification()(implicit executionContext: ExecutionContext): Future[Seq[User]] = Future.successful(userList)
}
