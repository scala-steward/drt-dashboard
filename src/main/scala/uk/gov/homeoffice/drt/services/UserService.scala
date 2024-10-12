package uk.gov.homeoffice.drt.services

import uk.gov.homeoffice.drt.db.{IUserDao, UserRow}

import scala.concurrent.{ExecutionContext, Future}

case class UserService(userDao: IUserDao) {
  def getUsers()(implicit ec: ExecutionContext): Future[Seq[UserRow]] = {
    userDao.selectAll
  }

  def getInactiveUsers(numberOfInactivityDays: Int)(implicit ec: ExecutionContext): Future[Seq[UserRow]] = {
    userDao.selectInactiveUsers(numberOfInactivityDays)
  }

  def getUsersToRevoke(numberOfInactivityDays: Int, deactivateAfterWarningDays: Int)(implicit ec: ExecutionContext): Future[Seq[UserRow]] = {
    userDao.selectUsersToRevokeAccess(numberOfInactivityDays, deactivateAfterWarningDays)
  }

  def upsertUser(userData: UserRow, purpose: Option[String])(implicit ec: ExecutionContext): Future[Int] = {
    userDao.upsertUser(userData, purpose)
  }

  def getUsersWithoutDropInNotification(implicit ec: ExecutionContext): Future[Seq[UserRow]] = {
    userDao.getUsersWithoutDropInNotification()
  }

}
