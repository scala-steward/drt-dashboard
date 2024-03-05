package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import spray.json.{RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.db.{DropIn, DropInDao, DropInRow}
import uk.gov.homeoffice.drt.json.DefaultTimeJsonProtocol

import java.sql.Timestamp
import scala.concurrent.{ExecutionContext, Future}

case class DropInPublished(published: Boolean)

case class DropInData(title: String, startTime: Long, endTime: Long, meetingLink: String)

trait DropInJsonFormats extends DefaultTimeJsonProtocol {

  implicit val dropInDataFormatParser: RootJsonFormat[DropInData] = jsonFormat4(DropInData)
  implicit val dropInFormatParser: RootJsonFormat[DropIn] = jsonFormat7(DropIn)
  implicit val dropInPublishedFormatParser: RootJsonFormat[DropInPublished] = jsonFormat1(DropInPublished)

}

object DropInSessionsRoute extends BaseRoute with DropInJsonFormats {
  override val log: Logger = LoggerFactory.getLogger(getClass)
  val longToTimestamp: Long => Timestamp = timeString => {
    new Timestamp(timeString)
  }


  private def dropsInsWithTimeStamp(dropIn: DropInRow): DropIn = {
    DropIn(dropIn.id, dropIn.title, dropIn.startTime.getTime, dropIn.endTime.getTime, dropIn.isPublished, dropIn.meetingLink, dropIn.lastUpdatedAt.getTime)
  }

  def updateDropIn(dropInDao: DropInDao, id: String)(implicit ec: ExecutionContext): Route =
    put {
      entity(as[DropInData]) { dropIn =>
        val updatedDropInResult = dropInDao.updateDropIn(DropInRow(Some(id.toInt), dropIn.title, longToTimestamp(dropIn.startTime), longToTimestamp(dropIn.endTime), false, Option(dropIn.meetingLink), new Timestamp(new DateTime().getMillis)))
        routeResponse(updatedDropInResult
                        .map(_ => complete(StatusCodes.OK, s"Drop-In with Id $id is updated successfully")), "Editing Drop-In")
      }
    }

  def publishDropIn(dropInDao: DropInDao, id: String)(implicit ec: ExecutionContext): Route =
    post {
      entity(as[DropInPublished]) { featurePublished =>
        val publishedSeminarResult = dropInDao.updatePublishDropIn(id, featurePublished.published)
          .map(_ => complete(StatusCodes.OK, s"Drop-In $id is published successfully"))

        routeResponse(publishedSeminarResult, "Publishing Drop-In")
      }
    }

  def deleteSession(dropInDao: DropInDao, id: String)(implicit ec: ExecutionContext): Route =
    delete {
      val deletedSeminarResult = dropInDao.deleteDropIn(id)
      routeResponse(deletedSeminarResult.map(_ => complete(StatusCodes.OK, s"Drop-In $id is deleted successfully")), "Deleting Drop-In")
    }

  def getSession(dropInDao: DropInDao, id: String)(implicit ec: ExecutionContext): Route =
    get {
      val getDropInResult = dropInDao.getDropIn(id).map(dropsInsWithTimeStamp).map(dropIn => complete(StatusCodes.OK, dropIn.toJson))
      routeResponse(getDropInResult, "Getting drop-in")
    }

  def getSessions(dropInDao: DropInDao)(implicit ec: ExecutionContext): Route =
    parameters("list-all".as[Boolean].optional) { listAll =>
      get {
        val dropIns: Future[Seq[DropInRow]] = if (listAll.contains(true)) dropInDao.getDropIns
        else dropInDao.getFutureDropIns

        val dropInsWithUTCtoLocalTime = dropIns.map(_.map(dropsInsWithTimeStamp))

        val getDropInsResult = dropInsWithUTCtoLocalTime.map(forms => complete(StatusCodes.OK, forms.toJson))
        routeResponse(getDropInsResult, "Getting drop-ins")
      }
    }

  def saveSession(dropInDao: DropInDao)(implicit ec: ExecutionContext): Route =
    post {
      entity(as[DropInData]) { dropIn =>
        val saveDropInResult = dropInDao
          .insertDropIn(dropIn.title, longToTimestamp(dropIn.startTime), longToTimestamp(dropIn.endTime), Option(dropIn.meetingLink))
        routeResponse(
          saveDropInResult.map(_ => complete(StatusCodes.OK, s"Drop_in ${dropIn.title} is saved successfully")), "Saving drop_in")
      }
    }

  def apply(dropInDao: DropInDao)(implicit ec: ExecutionContext): Route =
    pathPrefix("drop-in-sessions") {
      concat(
        pathEnd(
          concat(
            getSessions(dropInDao),
            saveSession(dropInDao)
            )
          ),
        path(Segment) { id =>
          concat(
            getSession(dropInDao, id),
            updateDropIn(dropInDao, id),
            deleteSession(dropInDao, id),
            )
        },
        path("update-published" / Segment)(id => publishDropIn(dropInDao, id))
        )
    }
}
