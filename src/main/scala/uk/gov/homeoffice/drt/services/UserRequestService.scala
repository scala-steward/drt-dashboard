package uk.gov.homeoffice.drt.services

import org.joda.time.DateTime
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.{ AccessRequest, ClientUserRequestedAccessData }
import uk.gov.homeoffice.drt.db.{ IUserAccessRequestDao, UserAccessRequestDao }

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global

class UserRequestService(userAccessRequestDao: IUserAccessRequestDao) {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def saveUserRequest(email: String, accessRequest: AccessRequest) = {
    val userAccessRequest = userAccessRequestDao.getUserAccessRequest(email, accessRequest, new Timestamp(DateTime.now().getMillis), "Requested")
    userAccessRequestDao.insertOrUpdate(userAccessRequest)
  }

  def updateUserRequest(clientUserRequestedAccessData: ClientUserRequestedAccessData, status: String) = {
    userAccessRequestDao.insertOrUpdate(clientUserRequestedAccessData.convertUserAccessRequest.copy(status = status))
  }

  def getUserRequest(status: String) = {
    userAccessRequestDao.selectForStatus(status)
  }

}
