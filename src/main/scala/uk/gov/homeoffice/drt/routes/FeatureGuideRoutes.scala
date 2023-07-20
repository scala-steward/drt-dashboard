package uk.gov.homeoffice.drt.routes


import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.slf4j.{Logger, LoggerFactory}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, RootJsonFormat, deserializationError, enrichAny}
import uk.gov.homeoffice.drt.db.FeatureGuideRow
import uk.gov.homeoffice.drt.services.s3.{S3Downloader, S3Service, S3Uploader}
import uk.gov.homeoffice.drt.uploadTraining.FeatureGuideService

import java.sql.Timestamp
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class FeaturePublished(published: Boolean)

trait FeatureGuideJsonFormats extends DefaultJsonProtocol {
  implicit object TimestampFormat extends JsonFormat[Timestamp] {
    override def write(obj: Timestamp): JsValue = JsString(obj.toString)

    override def read(json: JsValue): Timestamp = json match {
      case JsString(rawDate) => {
        try {
          Timestamp.valueOf(rawDate)
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

  implicit val featureGuideRowFormatParser: RootJsonFormat[FeatureGuideRow] = jsonFormat6(FeatureGuideRow)

  implicit val featurePublishedFormatParser: RootJsonFormat[FeaturePublished] = jsonFormat1(FeaturePublished)

}

object FeatureGuideRoutes extends FeatureGuideJsonFormats {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def routeResponse(responseF: Future[StandardRoute]): Route = {
    onComplete(responseF) {
      case Success(result) => result
      case Failure(ex) =>
        log.error(s"Error while uploading", ex)
        complete(StatusCodes.InternalServerError, ex.getMessage)
    }
  }

  def getFeatureVideoFile(downloader: S3Downloader, prefixFolder: String)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]) =
    path("get-feature-videos" / Segment) { filename =>
      get {
        val responseStreamF: Future[Source[ByteString, Future[IOResult]]] = downloader.download(s"$prefixFolder/$filename")

        val fileEntityF: Future[ResponseEntity] = responseStreamF.map(responseStream =>
          HttpEntity.Chunked(
            contentType = ContentTypes.`application/octet-stream`,
            chunks = responseStream.map(ChunkStreamPart.apply(_: ByteString))))

        val contentDispositionHeader: HttpHeader =
          RawHeader("Content-Disposition", s"attachment; filename=$filename")

        val responseF = fileEntityF.map { fileEntity => complete(HttpResponse(entity = fileEntity, headers = List(contentDispositionHeader))) }

        routeResponse(responseF)
      }
    }

  def getFeatureGuides(featureGuideService: FeatureGuideService)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]) = path("getFeatureGuides") {
    val responseF: Future[StandardRoute] = featureGuideService.getFeatureGuides().map { featureGuides =>
      val json: JsValue = featureGuides.toJson
      complete(StatusCodes.OK, json)
    }

    routeResponse(responseF)

  }

  def updateFeatureGuide(featureGuideService: FeatureGuideService)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]) =
    path("updateFeatureGuide" / Segment) { featureId =>
      post {
        entity(as[Multipart.FormData]) { _ =>
          formFields('title, 'markdownContent) { (title, markdownContent) =>
            val responseF = featureGuideService.updateFeatureGuide(featureId, title, markdownContent)
              .map(_ => complete(StatusCodes.OK, s"Feature $featureId is updated successfully"))

            routeResponse(responseF)
          }
        }
      }
    }

  def publishFeatureGuide(featureGuideService: FeatureGuideService)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]) =
    path("published" / Segment) { (featureId) =>
      post {
        entity(as[FeaturePublished]) { featurePublished =>
          val responseF = featureGuideService.updatePublishFeatureGuide(featureId, featurePublished.published)
            .map(_ => complete(StatusCodes.OK, s"Feature $featureId is published successfully"))

          routeResponse(responseF)

        }
      }
    }

  def deleteFeature(featureGuideService: FeatureGuideService)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]) =
    path("removeFeatureGuide" / Segment) { featureId =>
      delete {
        val responseF: Future[StandardRoute] = featureGuideService.deleteFeatureGuide(featureId).map { featureGuides =>
          val json: JsValue = featureGuides.toJson
          complete(StatusCodes.OK, json)
        }

        routeResponse(responseF)
      }
    }

  def apply(prefix: String, featureGuideService: FeatureGuideService, uploader: S3Uploader, downloader: S3Downloader, prefixFolder: String)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]) =
    pathPrefix(prefix) {
      concat(
        path("uploadFeatureGuide") {
          post {
            entity(as[Multipart.FormData]) { _ =>
              formFields('title, 'markdownContent) { (title, markdownContent) =>
                fileUpload("webmFile") {
                  case (metadata, byteSource) =>
                    val filename = metadata.fileName
                    featureGuideService.insertFeatureGuide(filename, title, markdownContent)
                    val responseF = uploader.upload(filename, byteSource)
                      .map(_ => complete(StatusCodes.OK, s"File $filename uploaded successfully"))
                    routeResponse(responseF)
                }
              }
            }
          }
        } ~ getFeatureGuides(featureGuideService) ~ deleteFeature(featureGuideService) ~ updateFeatureGuide(featureGuideService) ~ getFeatureVideoFile(downloader, prefixFolder) ~ publishFeatureGuide(featureGuideService))
    }
}
