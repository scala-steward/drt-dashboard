package uk.gov.homeoffice.drt.db

import scala.concurrent.{ ExecutionContext, Future }

class MockUserDao() extends IUserDao {
  var userList = Seq.empty[User]

  override def insertOrUpdate(userData: User): Future[Int] = {
    userList = userList :+ userData
    Future.successful(1)
  }

  override def selectInactiveUsers(numberOfInactivityDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[User]] = Future.successful(userList)

  override def selectUsersToRevokeAccess()(implicit executionContext: ExecutionContext): Future[Seq[User]] = {
    Future.successful(userList)
  }

  override def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[User]] = {
    Future.successful(userList)
  }
}
