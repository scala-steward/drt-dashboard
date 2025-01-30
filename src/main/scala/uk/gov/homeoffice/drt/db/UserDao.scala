package uk.gov.homeoffice.drt.db

import org.slf4j.{Logger, LoggerFactory}
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{TableQuery, Tag}
import spray.json.RootJsonFormat

import java.sql.Timestamp
import java.time.{Duration, Instant, LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

trait UserRowJsonSupport extends DateTimeJsonSupport {
  implicit val userFormatParser: RootJsonFormat[UserRow] = jsonFormat8(UserRow)
}

case class UserRow(
  id: String,
  username: String,
  email: String,
  latest_login: java.sql.Timestamp,
  inactive_email_sent: Option[java.sql.Timestamp],
  revoked_access: Option[java.sql.Timestamp],
  drop_in_notification_at: Option[java.sql.Timestamp],
  created_at: Option[java.sql.Timestamp])

class UserTable(tag: Tag, tableName: String = "user") extends Table[UserRow](tag, tableName) {

  def id = column[String]("id", O.PrimaryKey)

  def username = column[String]("username")

  def email = column[String]("email")

  def latest_login = column[java.sql.Timestamp]("latest_login")

  def inactive_email_sent = column[Option[java.sql.Timestamp]]("inactive_email_sent")

  def revoked_access = column[Option[java.sql.Timestamp]]("revoked_access")

  def drop_in_notification_at = column[Option[java.sql.Timestamp]]("drop_in_notification_at")

  def created_at = column[Option[java.sql.Timestamp]]("created_at")

  def * = (id, username, email, latest_login, inactive_email_sent, revoked_access, drop_in_notification_at, created_at) <> (UserRow.tupled, UserRow.unapply)

}

trait IUserDao {

  def upsertUser(user: UserRow, purpose: Option[String])(implicit ec: ExecutionContext): Future[Int]

  def selectInactiveUsers(numberOfInactivityDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[UserRow]]

  def selectUsersToRevokeAccess(numberOfInactivityDays: Int, deactivateAfterWarningDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[UserRow]]

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserRow]]

  def getUsersWithoutDropInNotification()(implicit executionContext: ExecutionContext): Future[Seq[UserRow]]

}

case class UserDao(db: CentralDatabase) extends IUserDao {
  val log: Logger = LoggerFactory.getLogger(getClass)

  val userTable: TableQuery[UserTable] = TableQuery[UserTable]

  val secondsInADay = 60 * 60 * 24

  def noActivitySinceDays(numberOfInactivityDays: Int): UserTable => Rep[Boolean] = (user: UserTable) =>
    user.inactive_email_sent.isEmpty &&
      user.latest_login < new Timestamp(Instant.now().minusSeconds(numberOfInactivityDays * secondsInADay).toEpochMilli)

  def accessShouldBeRevoked(numberOfInactivityDays: Int, deactivateAfterWarningDays: Int): UserTable => Rep[Boolean] = (user: UserTable) =>
    user.revoked_access.isEmpty &&
      user.latest_login < new Timestamp(Instant.now().minusSeconds((numberOfInactivityDays + deactivateAfterWarningDays) * secondsInADay).toEpochMilli) &&
      user.inactive_email_sent.map(_ < new Timestamp(Instant.now().minusSeconds((deactivateAfterWarningDays) * secondsInADay).toEpochMilli)).getOrElse(false)

  private def insertOrUpdate(userData: UserRow): Future[Int] = {
    db.run(userTable insertOrUpdate userData)
  }

  def selectInactiveUsers(numberOfInactivityDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[UserRow]] = {
    val inactiveIdx: UserTable => PostgresProfile.api.Rep[Boolean] = noActivitySinceDays(numberOfInactivityDays)
    db.run(userTable.filter(inactiveIdx).result)
      .mapTo[Seq[UserRow]]
  }

  def selectUsersToRevokeAccess(numberOfInactivityDays: Int, deactivateAfterWarningDays: Int)(implicit executionContext: ExecutionContext): Future[Seq[UserRow]] = {
    val revokeIdx: UserTable => PostgresProfile.api.Rep[Boolean] = accessShouldBeRevoked(numberOfInactivityDays, deactivateAfterWarningDays)
    db.run(userTable.filter(revokeIdx).result)
      .mapTo[Seq[UserRow]]
  }

  def selectAll()(implicit executionContext: ExecutionContext): Future[Seq[UserRow]] = {
    db.run(userTable.result).mapTo[Seq[UserRow]]
  }

  override def getUsersWithoutDropInNotification()(implicit executionContext: ExecutionContext): Future[Seq[UserRow]] = {
    val specificDate = Timestamp.from(LocalDateTime.of(2023, 9, 1, 0, 0).toInstant(ZoneOffset.UTC))
    val fifteenDaysAgo = Timestamp.from(Instant.now.minus(Duration.ofDays(15)))

    db.run(userTable.filter(u =>
      u.created_at > specificDate &&
        u.created_at < fifteenDaysAgo &&
        u.drop_in_notification_at.isEmpty &&
        u.revoked_access.isEmpty
    ).result).mapTo[Seq[UserRow]]
  }

  def upsertUser(user: UserRow, purpose: Option[String])(implicit ec: ExecutionContext): Future[Int] = {
    for {
      updateCount <- insertOrUpdateUser(user, purpose)
      result <- if (updateCount > 0) Future.successful(updateCount) else insertOrUpdate(user)
    } yield result
  }.recover {
    case throwable =>
      log.error("Upsert failed", throwable)
      0
  }

  private def insertOrUpdateUser(user: UserRow, purpose: Option[String]): Future[Int] = {
    val query = purpose match {
      case Some(p) if p == "userTracking" =>
        userTable.filter(_.email === user.email)
          .map(f => (f.latest_login, f.inactive_email_sent, f.revoked_access))
          .update(user.latest_login, user.inactive_email_sent, user.revoked_access)
      case Some(p) if p == "inactivity" =>
        userTable.filter(_.email === user.email)
          .map(f => (f.inactive_email_sent, f.revoked_access))
          .update(user.inactive_email_sent, user.revoked_access)
      case Some(p) if p == "revoked" =>
        userTable.filter(_.email === user.email)
          .map(f => (f.revoked_access))
          .update(user.revoked_access)
      case Some(p) if p == "dropInNotification" =>
        userTable.filter(_.email === user.email)
          .map(f => (f.drop_in_notification_at))
          .update(user.drop_in_notification_at)
      case Some(p) if p == "Approved" =>
        userTable.filter(_.email === user.email)
          .map(f => (f.created_at))
          .update(user.created_at)
      case _ => userTable.insertOrUpdate(user)
    }
    db.run(query)

  }
}
