package uk.gov.homeoffice.drt.db

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeEach
import slick.dbio.DBIO
import slick.jdbc.PostgresProfile.api._

import java.sql.Timestamp
import java.time.Instant
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class UserAccessRequestDaoSpec extends Specification with BeforeEach {
  sequential

  lazy val db = TestDatabase.db

  override protected def before = {
    Await.ready(
      db.run(DBIO.seq(
        TestDatabase.userAccessRequestsTable.schema.dropIfExists,
        TestDatabase.userAccessRequestsTable.schema.createIfNotExists)
      ), 2.second)
  }

  def getUserAccessRequest(requestTime: Timestamp) = {
    UserAccessRequest(email = "test@test.com",
      portsRequested = "",
      allPorts = false,
      regionsRequested = "",
      staffEditing = false,
      lineManager = "",
      agreeDeclaration = true,
      accountType = "port",
      portOrRegionText = "lhr",
      staffText = "",
      status = "Requested",
      requestTime = requestTime)
  }

  "UserAccessRequestDao list" >> {
    "should return a list of user Access Requested" >> {
      val userAccessRequestDao = UserAccessRequestDao(TestDatabase.db)
      val userAccessRequest = getUserAccessRequest(new Timestamp(Instant.now().minusSeconds(60).toEpochMilli))

      Await.result(userAccessRequestDao.insertOrUpdate(userAccessRequest), 1.second)
      val userAccessRequests = Await.result(userAccessRequestDao.selectAll(), 1.second)

      userAccessRequests.size mustEqual 1
      userAccessRequests.head mustEqual userAccessRequest
    }
  }
}
