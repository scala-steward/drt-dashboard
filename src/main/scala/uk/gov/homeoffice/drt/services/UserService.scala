package uk.gov.homeoffice.drt.services

import uk.gov.homeoffice.drt.db.{ IUserDao, User }

import scala.concurrent.{ ExecutionContext, Future }

class UserService(userDao: IUserDao) {
  def getUsers()(implicit ec: ExecutionContext): Future[Seq[User]] = {
    userDao.selectAll
  }

  def getInactiveUsers(numberOfInactivityDays: Int)(implicit ec: ExecutionContext): Future[Seq[User]] = {
    userDao.selectInactiveUsers(numberOfInactivityDays)
  }

  def getUsersToRevoke()(implicit ec: ExecutionContext): Future[Seq[User]] = {
    userDao.selectUsersToRevokeAccess()
  }

  def upsertUser(userData: User)(implicit ec: ExecutionContext): Future[Int] = {
    userDao.insertOrUpdate(userData)
  }

}
