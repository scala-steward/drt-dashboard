package uk.gov.homeoffice.drt.db

import org.joda.time.DateTime
import slick.jdbc.PostgresProfile.api._
import slick.lifted.ProvenShape
import java.sql.Timestamp
import scala.concurrent.Future

case class DropInRegistrationRow(email: String,
                                 dropInId: Int,
                                 registeredAt: Timestamp,
                                 emailSentAt: Option[Timestamp])

class DropInRegistrationTable(tag: Tag) extends Table[DropInRegistrationRow](tag, "drop_in_registration") {

  def email: Rep[String] = column[String]("email")

  def dropInId: Rep[Int] = column[Int]("drop_in_id")

  def registeredAt: Rep[Timestamp] = column[Timestamp]("registered_at")

  def emailSentAt: Rep[Option[Timestamp]] = column[Option[Timestamp]]("email_sent_at")

  def * : ProvenShape[DropInRegistrationRow] = (email, dropInId, registeredAt, emailSentAt).mapTo[DropInRegistrationRow]

  val pk = primaryKey("drop_in_registration_pkey", (email, dropInId))

}


case class DropInRegistrationDao(db: Database) {
  val dropInRegistrationTable = TableQuery[DropInRegistrationTable]

  private def getCurrentTime = new Timestamp(new DateTime().getMillis)

  def updateEmailSentTime(dropInId: String) = {
    val query = dropInRegistrationTable.filter(_.dropInId === dropInId.trim.toInt).map(f => (f.emailSentAt))
      .update(Some(getCurrentTime))
    db.run(query)
  }

  def removeRegisteredUser(dropInId: String, email: String): Future[Int] = {
    val query = dropInRegistrationTable.filter(r => r.dropInId === dropInId.trim.toInt && r.email === email.trim).delete
    db.run(query)
  }

  def getRegisteredUsers(dropInId: String): Future[Seq[DropInRegistrationRow]] = {
    val query = dropInRegistrationTable.filter(_.dropInId === dropInId.trim.toInt).sortBy(_.registeredAt.desc).result
    val result = db.run(query)
    result
  }

  def getRegisteredUsersToNotify(dropInId: String, dropInDate: Timestamp): Future[Seq[DropInRegistrationRow]] = {

    val sevenDaysMilliSeconds = 7L * 24L * 60L * 60L * 1000L
    val numberOfDaysBeforeSeminar = new Timestamp(dropInDate.getTime - sevenDaysMilliSeconds)

    val query = dropInRegistrationTable
      .filter(r => r.dropInId === dropInId.trim.toInt && r.emailSentAt.map(es => es < numberOfDaysBeforeSeminar).getOrElse(true))
      .sortBy(_.registeredAt.desc).result

    val result = db.run(query)
    result
  }

  def findRegistrationsByEmail(email: String): Future[Seq[DropInRegistrationRow]] = {
    val query = dropInRegistrationTable.filter(r => r.email === email.trim).result
    val result = db.run(query)
    result
  }

  def insertRegistration(email:String, dropInId: Int, registeredAt: Timestamp, emailSentAt: Option[Timestamp]): Future[Int] = {
    val insertAction = dropInRegistrationTable += DropInRegistrationRow(email, dropInId, registeredAt, emailSentAt)
    db.run(insertAction)
  }
}
