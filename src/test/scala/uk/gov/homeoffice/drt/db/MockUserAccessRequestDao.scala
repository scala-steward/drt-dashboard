package uk.gov.homeoffice.drt.db

import scala.concurrent.{ ExecutionContext, Future }

class MockUserAccessRequestDao extends IUserAccessRequestDao {
  var userAccessRequestList = Seq.empty[UserAccessRequest]

  override def insertOrUpdate(userAccessRequest: UserAccessRequest): Future[Int] = {
    userAccessRequestList = userAccessRequestList :+ userAccessRequest
    Future.successful(1)
  }

  override def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserAccessRequest]] = Future.successful(userAccessRequestList)

  override def selectForStatus(status: String): Future[Seq[UserAccessRequest]] = Future.successful(userAccessRequestList)
}
