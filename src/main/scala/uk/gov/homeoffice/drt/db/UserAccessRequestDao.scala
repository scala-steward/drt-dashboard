package uk.gov.homeoffice.drt.db

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.DateTime
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ TableQuery, Tag }
import spray.json.{ DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError }
import uk.gov.homeoffice.drt.authentication.AccessRequest

import java.sql.Timestamp
import scala.concurrent.{ ExecutionContext, Future }

trait UserAccessRequestJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit object DateTimeFormat extends JsonFormat[Timestamp] {
    override def write(obj: Timestamp): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Timestamp = json match {
      case JsString(rawDate) => {
        try {
          DateTime.parse(rawDate)
        } catch {
          case iae: IllegalArgumentException => deserializationError("Invalid date format")
          case _: Exception => None
        }
      } match {
        case dateTime: Timestamp => dateTime
        case None => deserializationError(s"Couldn't parse date time, got $rawDate")
      }

    }
  }

  implicit val userAccessRequestFormatParser: RootJsonFormat[UserAccessRequest] = jsonFormat12(UserAccessRequest)
}

case class UserAccessRequest(
  email: String,
  portsRequested: String,
  allPorts: Boolean,
  regionsRequested: String,
  staffEditing: Boolean,
  lineManager: String,
  agreeDeclaration: Boolean,
  accountType: String,
  portOrRegionText: String,
  staffText: String,
  status: String,
  requestTime: java.sql.Timestamp)

class UserAccessRequestsTable(tag: Tag) extends Table[UserAccessRequest](tag, "user_access_requests") {

  def email = column[String]("email", O.PrimaryKey)

  def allPorts = column[Boolean]("all_ports")

  def portsRequested = column[String]("ports")

  def regionsRequested = column[String]("regions")

  def staffing = column[Boolean]("staff_editing")

  def lineManager = column[String]("line_manager")

  def agreeDeclaration = column[Boolean]("agree_declaration")

  def accountType = column[String]("account_type")

  def portOrRegionText = column[String]("port_or_region_text")

  def staffText = column[String]("staff_text")

  def status = column[String]("status")

  def requestTime = column[java.sql.Timestamp]("request_time", O.PrimaryKey)

  def * = (email, portsRequested, allPorts, regionsRequested, staffing, lineManager, agreeDeclaration, accountType, portOrRegionText, staffText, status, requestTime).mapTo[UserAccessRequest]
}

trait IUserAccessRequestDao {
  def getUserAccessRequest(email: String, accessRequest: AccessRequest, timestamp: java.sql.Timestamp, status: String): UserAccessRequest = {
    UserAccessRequest(
      email = email,
      portsRequested = accessRequest.portsRequested.mkString(","),
      allPorts = accessRequest.allPorts,
      regionsRequested = accessRequest.regionsRequested.mkString(","),
      staffEditing = accessRequest.staffing,
      lineManager = accessRequest.lineManager,
      agreeDeclaration = accessRequest.agreeDeclaration,
      accountType = accessRequest.rccOption,
      portOrRegionText = accessRequest.portOrRegionText,
      staffText = accessRequest.staffText,
      status = status,
      requestTime = timestamp)
  }

  def insertOrUpdate(userAccessRequest: UserAccessRequest): Future[Int]

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserAccessRequest]]

  def selectForStatus(status: String): Future[Seq[UserAccessRequest]]

}

class UserAccessRequestDao(db: Database, userAccessRequests: TableQuery[UserAccessRequestsTable]) extends IUserAccessRequestDao {

  def insertOrUpdate(userAccessRequest: UserAccessRequest): Future[Int] = {
    db.run(userAccessRequests insertOrUpdate userAccessRequest)
  }

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserAccessRequest]] = {
    db.run(userAccessRequests.result).mapTo[Seq[UserAccessRequest]]
  }

  def selectForStatus(status: String): Future[Seq[UserAccessRequest]] = {
    db.run(userAccessRequests.filter(_.status === status).result)
  }
}