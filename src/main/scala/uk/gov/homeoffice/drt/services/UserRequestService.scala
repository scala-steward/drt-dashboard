package uk.gov.homeoffice.drt.services

import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.{ AccessRequest, ClientUserRequestedAccessData }
import uk.gov.homeoffice.drt.db.{ IUserAccessRequestDao, UserAccessRequest }

import java.sql.Timestamp
import scala.concurrent.Future

case class UserRequestService(userAccessRequestDao: IUserAccessRequestDao) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def saveUserRequest(email: String, accessRequest: AccessRequest, timestamp: Timestamp): Future[Int] = {
    val userAccessRequest = userAccessRequestDao.userAccessRequest(email, accessRequest, timestamp, "Requested")
    userAccessRequestDao.insertOrUpdate(userAccessRequest)
  }

  def updateUserRequest(clientUserRequestedAccessData: ClientUserRequestedAccessData, status: String): Future[Int] = {
    userAccessRequestDao.insertOrUpdate(clientUserRequestedAccessData.convertUserAccessRequest.copy(status = status))
  }

  def getUserRequest(status: String): Future[Seq[UserAccessRequest]] = {
    userAccessRequestDao.selectForStatus(status)
  }

  def getUserRequestByEmail(email:String): Future[Seq[UserAccessRequest]] = {
    userAccessRequestDao.selectByEmail(email)
  }

}
