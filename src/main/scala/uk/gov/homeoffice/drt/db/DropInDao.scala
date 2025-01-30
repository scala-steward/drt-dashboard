package uk.gov.homeoffice.drt.db

import org.joda.time.DateTime
import slick.lifted.ProvenShape
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch

import java.sql.Timestamp
import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}


case class DropIn(id: Option[Int],
                  title: String,
                  startTime: MillisSinceEpoch,
                  endTime: MillisSinceEpoch,
                  isPublished: Boolean,
                  meetingLink: Option[String],
                  lastUpdatedAt: MillisSinceEpoch)

case class DropInRow(id: Option[Int],
                     title: String,
                     startTime: Timestamp,
                     endTime: Timestamp,
                     isPublished: Boolean,
                     meetingLink: Option[String],
                     lastUpdatedAt: Timestamp)

class DropInTable(tag: Tag) extends Table[DropInRow](tag, "drop_in") {
  def id: Rep[Option[Int]] = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)

  def title: Rep[String] = column[String]("title")

  def startTime: Rep[Timestamp] = column[Timestamp]("start_time")

  def endTime: Rep[Timestamp] = column[Timestamp]("end_time")

  def isPublished: Rep[Boolean] = column[Boolean]("is_published")

  def meetingLink: Rep[Option[String]] = column[Option[String]]("meeting_link")

  def lastUpdatedAt: Rep[Timestamp] = column[Timestamp]("last_updated_at")

  def * : ProvenShape[DropInRow] = (id, title, startTime, endTime, isPublished, meetingLink, lastUpdatedAt).mapTo[DropInRow]
}

object DropInDao {
  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

  val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

  val zonedUKDateTime: Timestamp => ZonedDateTime = timestamp => timestamp.toInstant.atZone(ZoneId.of("Europe/London"))

  def getUKStringDate(timestamp: Timestamp, formatter: DateTimeFormatter): String = zonedUKDateTime(timestamp).format(formatter)

  def getDate(startTime: Timestamp): String = getUKStringDate(startTime, dateFormatter)

  def getStartTime(startTime: Timestamp): String = getUKStringDate(startTime, timeFormatter)

  def getEndTime(endTime: Timestamp): String = getUKStringDate(endTime, timeFormatter)
}

case class DropInDao(db: CentralDatabase) {
  val dropInTable = TableQuery[DropInTable]

  private def getCurrentTime = new Timestamp(new DateTime().getMillis)

  def updatePublishDropIn(dropInId: String, publish: Boolean) = {
    val query = dropInTable.filter(_.id === dropInId.trim.toInt).map(f => (f.isPublished, f.lastUpdatedAt))
      .update(publish, getCurrentTime)
    db.run(query)
  }

  def updateDropIn(dropInRow: DropInRow): Future[Int] = dropInRow.id match {
    case Some(id) =>
      val query = dropInTable.filter(_.id === id).map(f => (f.title, f.startTime, f.endTime, f.meetingLink, f.lastUpdatedAt))
        .update(dropInRow.title, dropInRow.startTime, dropInRow.endTime, dropInRow.meetingLink, getCurrentTime)
      db.run(query)
    case None => Future.successful(0)
  }

  def deleteDropIn(dropInId: String): Future[Int] = {
    val query = dropInTable.filter(_.id === dropInId.trim.toInt).delete
    db.run(query)
  }

  def getDropInDueForNotifying(notifyDate: Long, presentDate: Long): Future[Seq[DropInRow]] = {
    val query = dropInTable
      .filter(r => r.startTime > new Timestamp(presentDate) && r.startTime < new Timestamp(notifyDate))
      .sortBy(_.startTime).result
    val result = db.run(query)
    result
  }

  def getDropIns: Future[Seq[DropInRow]] = {
    val query = dropInTable.sortBy(_.startTime).result
    val result = db.run(query)
    result
  }

  def getDropIn(dropInId: String)(implicit ec: ExecutionContext): Future[DropInRow] = {
    val query = dropInTable.filter(_.id === dropInId.trim.toInt).result
    val result = db.run(query)
    result.map(_.head)
  }

  def getFutureDropIns: Future[Seq[DropInRow]] = {
    val query = dropInTable.filter(_.startTime > new Timestamp(DateTime.now().withTimeAtStartOfDay().minusDays(1).getMillis)).sortBy(_.startTime).result
    val result = db.run(query)
    result
  }

  def insertDropIn(title: String, startTime: Timestamp, endTime: Timestamp, meetingLink: Option[String]): Future[Int] = {
    val insertAction = dropInTable += DropInRow(None, title, startTime, endTime, false, meetingLink, getCurrentTime)
    db.run(insertAction)
  }
}
