package uk.gov.homeoffice.drt.db

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.DateTime
import org.slf4j.{ Logger, LoggerFactory }
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ TableQuery, Tag }
import spray.json.{ DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError }

import java.sql.Timestamp
import java.time.LocalDateTime
import scala.concurrent.{ ExecutionContext, Future }

trait UserJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
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

  implicit val userFormatParser: RootJsonFormat[User] = jsonFormat6(User)
}

case class User(
  id: String,
  username: String,
  email: String,
  latest_login: java.sql.Timestamp,
  inactive_email_sent: Option[java.sql.Timestamp],
  revoked_access: Option[java.sql.Timestamp])

class UserTable(tag: Tag) extends Table[User](tag, "user") {

  def id = column[String]("id", O.PrimaryKey)

  def username = column[String]("username")

  def email = column[String]("email")

  def latest_login = column[java.sql.Timestamp]("latest_login")

  def inactive_email_sent = column[Option[java.sql.Timestamp]]("inactive_email_sent")

  def revoked_access = column[Option[java.sql.Timestamp]]("revoked_access")

  def * = (id, username, email, latest_login, inactive_email_sent, revoked_access).mapTo[User]
}

trait IUserDao {
  def insertOrUpdate(userData: User): Future[Int]

  def selectInactiveUsers(numberOfInactivityDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[User]]

  def selectUsersToRevokeAccess()(implicit executionContext: ExecutionContext): Future[Seq[User]]

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[User]]

}

class UserDao(db: Database, userTable: TableQuery[UserTable]) extends IUserDao {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def insertOrUpdate(userData: User): Future[Int] = {
    db.run(userTable insertOrUpdate userData)
  }

  def selectInactiveUsers(numberOfInactivityDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[User]] = {
    db.run(userTable.result)
      .mapTo[Seq[User]]
      .map(_.filter(u => u.inactive_email_sent.isEmpty && u.latest_login.toLocalDateTime.isBefore(LocalDateTime.now().minusDays(numberOfInactivityDays))))
  }

  def selectUsersToRevokeAccess()(implicit executionContext: ExecutionContext): Future[Seq[User]] = {
    db.run(userTable.result)
      .mapTo[Seq[User]]
      .map(_.filter(u => u.revoked_access.isEmpty && u.inactive_email_sent.exists(_.toLocalDateTime.isBefore(LocalDateTime.now().minusDays(7)))))
  }

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[User]] = {
    db.run(userTable.result).mapTo[Seq[User]]
  }

}
