package uk.gov.homeoffice.drt.services

import org.joda.time.DateTime
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.{ AccessRequest, ClientUserRequestedAccessData }
import uk.gov.homeoffice.drt.db.UserAccessRequestDao

import java.sql.Timestamp
import scala.concurrent.ExecutionContext.Implicits.global

object UserRequestService {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def saveUserRequest(email: String, accessRequest: AccessRequest) = {
    log.info(s"request for access $email $accessRequest")
    val userAccessRequest = UserAccessRequestDao.getUserAccessRequest(email, accessRequest, new Timestamp(DateTime.now().getMillis), "Requested")
    UserAccessRequestDao.insertOrUpdate(userAccessRequest)
  }

  def updateUserRequest(clientUserRequestedAccessData: ClientUserRequestedAccessData, status: String) = {
    UserAccessRequestDao.insertOrUpdate(clientUserRequestedAccessData.convertUserAccessRequest.copy(status = status))
  }

  def getUserRequest(status: String) = {
    UserAccessRequestDao.selectForStatus(status)
  }

}
