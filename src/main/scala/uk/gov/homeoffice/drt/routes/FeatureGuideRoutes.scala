package uk.gov.homeoffice.drt.routes


import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpEntity.ChunkStreamPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.IOResult
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.slf4j.{Logger, LoggerFactory}
import spray.json.{JsValue, enrichAny}
import uk.gov.homeoffice.drt.json.FeatureGuideJsonFormats
import uk.gov.homeoffice.drt.services.s3.{S3Downloader, S3Uploader}
import uk.gov.homeoffice.drt.uploadTraining.FeatureGuideService

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class FeaturePublished(published: Boolean)

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

  def getFeatureVideoFile(downloader: S3Downloader)
                         (implicit ec: ExecutionContextExecutor): Route =
    path("get-feature-videos" / Segment) { filename =>
      get {
        val responseStreamF: Future[Source[ByteString, Future[IOResult]]] = downloader.download(filename)

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

  def getFeatureGuides(featureGuideService: FeatureGuideService)
                      (implicit ec: ExecutionContextExecutor): Route = path("getFeatureGuides") {
    val responseF: Future[StandardRoute] = featureGuideService.getFeatureGuides().map { featureGuides =>
      val json: JsValue = featureGuides.toJson
      complete(StatusCodes.OK, json)
    }

    routeResponse(responseF)

  }

  def updateFeatureGuide(featureGuideService: FeatureGuideService)
                        (implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route =
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

  def publishFeatureGuide(featureGuideService: FeatureGuideService)
                         (implicit ec: ExecutionContextExecutor): Route =
    path("published" / Segment) { (featureId) =>
      post {
        entity(as[FeaturePublished]) { featurePublished =>
          val responseF = featureGuideService.updatePublishFeatureGuide(featureId, featurePublished.published)
            .map(_ => complete(StatusCodes.OK, s"Feature $featureId is published successfully"))

          routeResponse(responseF)

        }
      }
    }

  def deleteFeature(featureGuideService: FeatureGuideService)
                   (implicit ec: ExecutionContextExecutor): Route =
    path("removeFeatureGuide" / Segment) { featureId =>
      delete {
        val responseF: Future[StandardRoute] = featureGuideService.deleteFeatureGuide(featureId).map { featureGuides =>
          val json: JsValue = featureGuides.toJson
          complete(StatusCodes.OK, json)
        }

        routeResponse(responseF)
      }
    }

  def apply(prefix: String, featureGuideService: FeatureGuideService, uploader: S3Uploader, downloader: S3Downloader)
           (implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route =
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
        }
          ~ getFeatureGuides(featureGuideService)
          ~ deleteFeature(featureGuideService)
          ~ updateFeatureGuide(featureGuideService)
          ~ getFeatureVideoFile(downloader)
          ~ publishFeatureGuide(featureGuideService))
    }
}
