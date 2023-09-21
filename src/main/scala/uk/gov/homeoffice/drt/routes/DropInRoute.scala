package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import org.joda.time.DateTime
import org.slf4j.{Logger, LoggerFactory}
import spray.json.{RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.db.{DropInDao, DropInRow}
import uk.gov.homeoffice.drt.json.DefaultTimeJsonProtocol

import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZoneOffset}
import scala.concurrent.ExecutionContext

case class DropInPublished(published: Boolean)

case class DropInData(title: String, startTime: String, endTime: String, meetingLink: String)

trait DropInJsonFormats extends DefaultTimeJsonProtocol {

  implicit val dropInDataFormatParser: RootJsonFormat[DropInData] = jsonFormat4(DropInData)
  implicit val dropInRowFormatParser: RootJsonFormat[DropInRow] = jsonFormat7(DropInRow)
  implicit val dropInPublishedFormatParser: RootJsonFormat[DropInPublished] = jsonFormat1(DropInPublished)

}

object DropInRoute extends BaseRoute with DropInJsonFormats {
  override val log: Logger = LoggerFactory.getLogger(getClass)

  val stringToTimestamp: String => Timestamp = timeString => {
    val localTime = LocalDateTime.parse(timeString, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"))
      .atZone(ZoneId.of("Europe/London"))

    val utcTime = localTime.withZoneSameInstant(ZoneOffset.UTC)

    new Timestamp(utcTime.toInstant.toEpochMilli)
  }

  def updateDropIn(dropInDao: DropInDao)(implicit ec: ExecutionContext) = path("update" / Segment) { dropInId =>
    put {
      entity(as[DropInData]) { dropIn =>
        val updatedDropInResult = dropInDao.updateDropIn(DropInRow(Some(dropInId.toInt), dropIn.title, stringToTimestamp(dropIn.startTime), stringToTimestamp(dropIn.endTime), false, Option(dropIn.meetingLink), new Timestamp(new DateTime().getMillis)))
        routeResponse(updatedDropInResult
          .map(_ => complete(StatusCodes.OK, s"Drop-In with Id $dropInId is updated successfully")), "Editing Drop-In")
      }
    }
  }

  def publishDropIn(dropInDao: DropInDao)(implicit ec: ExecutionContext) = path("published" / Segment) { (dropInId) =>
    post {
      entity(as[DropInPublished]) { featurePublished =>
        val publishedSeminarResult = dropInDao.updatePublishDropIn(dropInId, featurePublished.published)
          .map(_ => complete(StatusCodes.OK, s"Drop-In $dropInId is published successfully"))

        routeResponse(publishedSeminarResult, "Publishing Drop-In")

      }
    }
  }

  def deleteSeminar(dropInDao: DropInDao)(implicit ec: ExecutionContext) =
    path("delete" / Segment) { dropInId =>
      delete {
        val deletedSeminarResult = dropInDao.deleteDropIn(dropInId)
        routeResponse(deletedSeminarResult.map(_ => complete(StatusCodes.OK, s"Drop-In $dropInId is deleted successfully")), "Deleting Drop-In")
      }
    }

  def getDropIn(dropInDao: DropInDao)(implicit ec: ExecutionContext) = path("get" / Segment) { dropInId =>
    get {
      val getDropInResult =
        dropInDao.getDropIn(dropInId).map(dropIn => complete(StatusCodes.OK, dropIn.toJson))
      routeResponse(getDropInResult, "Getting drop-in")
    }
  }

  def getDropIns(dropInDao: DropInDao)(implicit ec: ExecutionContext) = path("getList" / Segment) { listAll =>
    get {
      val getDropInsResult =
        if (listAll.toBoolean) dropInDao.getDropIns.map(forms => complete(StatusCodes.OK, forms.toJson))
        else dropInDao.getFutureDropIns.map(forms => complete(StatusCodes.OK, forms.toJson))
      routeResponse(getDropInsResult, "Getting drop-ins")
    }
  }

  def saveDropIn(dropInDao: DropInDao)(implicit ec: ExecutionContext) = path("save") {
    post {
      entity(as[DropInData]) { dropIn =>
        val saveDropInResult = dropInDao.insertDropIn(dropIn.title, stringToTimestamp(dropIn.startTime), stringToTimestamp(dropIn.endTime), Option(dropIn.meetingLink))
        routeResponse(
          saveDropInResult.map(_ => complete(StatusCodes.OK, s"Drop_in ${dropIn.title} is saved successfully")), "Saving drop_in")
      }
    }
  }

  def apply(prefix: String, dropInDao: DropInDao)(implicit ec: ExecutionContext) = pathPrefix(prefix) {
    concat(saveDropIn(dropInDao) ~ getDropIn(dropInDao) ~ getDropIns(dropInDao) ~ deleteSeminar(dropInDao) ~ publishDropIn(dropInDao) ~ updateDropIn(dropInDao))
  }
}
