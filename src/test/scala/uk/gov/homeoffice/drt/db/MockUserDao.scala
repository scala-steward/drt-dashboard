package uk.gov.homeoffice.drt.db

import java.time.LocalDateTime
import scala.concurrent.{ ExecutionContext, Future }

class MockUserDao() extends IUserDao {
  var userList = Seq.empty[User]

  override def insertOrUpdate(userData: User): Future[Int] = {
    userList = userList :+ userData
    Future.successful(1)
  }

  override def selectInactiveUsers(numberOfInactivityDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[User]] =
    Future.successful(userList)
      .mapTo[Seq[User]]
      .map(_.filter(u => u.inactive_email_sent.isEmpty && u.latest_login.toLocalDateTime.isBefore(LocalDateTime.now().minusDays(numberOfInactivityDays))))

  override def selectUsersToRevokeAccess()(implicit executionContext: ExecutionContext): Future[Seq[User]] = {
    Future.successful(userList)
      .mapTo[Seq[User]]
      .map(_.filter(u => u.revoked_access.isEmpty && u.inactive_email_sent.exists(_.toLocalDateTime.isBefore(LocalDateTime.now().minusDays(7)))))
  }

  override def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[User]] = {
    Future.successful(userList)
  }

  def deleteAll()(implicit executionContext: ExecutionContext): Future[Int] = {
    userList = Seq.empty[User]
    Future.successful(userList.size)
  }

}
