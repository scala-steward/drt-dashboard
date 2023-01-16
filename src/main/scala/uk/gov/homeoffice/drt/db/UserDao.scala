package uk.gov.homeoffice.drt.db

import org.slf4j.{ Logger, LoggerFactory }
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ TableQuery, Tag }
import spray.json.RootJsonFormat

import java.time.LocalDateTime
import scala.concurrent.{ ExecutionContext, Future }

trait UserJsonSupport extends DateTimeJsonSupport {
  implicit val userFormatParser: RootJsonFormat[User] = jsonFormat6(User)
}

case class User(
  id: String,
  username: String,
  email: String,
  latest_login: java.sql.Timestamp,
  inactive_email_sent: Option[java.sql.Timestamp],
  revoked_access: Option[java.sql.Timestamp])

class UserTable(tag: Tag, tableName: String = "user") extends Table[User](tag, tableName) {

  def id = column[String]("ID", O.PrimaryKey)

  def username = column[String]("USERNAME")

  def email = column[String]("EMAIL")

  def latest_login = column[java.sql.Timestamp]("LATEST_LOGIN")

  def inactive_email_sent = column[Option[java.sql.Timestamp]]("INACTIVE_EMAIL_SENT")

  def revoked_access = column[Option[java.sql.Timestamp]]("REVOKED_ACCESS")

  def * = (id, username, email, latest_login, inactive_email_sent, revoked_access).mapTo[User]
}

trait IUserDao {
  def insertOrUpdate(userData: User): Future[Int]

  def selectInactiveUsers(numberOfInactivityDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[User]]

  def selectUsersToRevokeAccess()(implicit executionContext: ExecutionContext): Future[Seq[User]]

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[User]]

  def deleteAll()(implicit executionContext: ExecutionContext): Future[Int]
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

  override def deleteAll()(implicit executionContext: ExecutionContext): Future[Int] = {
    db.run(userTable.delete)
  }
}
