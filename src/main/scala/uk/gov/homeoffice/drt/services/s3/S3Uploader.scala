package uk.gov.homeoffice.drt.services.s3

import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.slf4j.{Logger, LoggerFactory}
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.model._

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}

case class S3Uploader(uploader: S3MultipartUploader, bucketName: String, prefix: Option[String]) {
  private val log = LoggerFactory.getLogger(getClass)

  def upload(objectKey: String, data: Source[ByteString, Any])
            (implicit mat: Materializer, ec: ExecutionContext): Future[CompleteMultipartUploadResponse] = {
    val fullObjectKey = prefix.map(p => s"$p/$objectKey").getOrElse(objectKey)
    val createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
      .bucket(bucketName)
      .key(fullObjectKey).build()

    uploader.createMultipartUpload(createMultipartUploadRequest)
      .flatMap { uploadResponse =>
        val uploadId = uploadResponse.uploadId()
        data
          .groupedWeighted(1024 * 1024 * 5)(_.length)
          .zipWithIndex
          .mapAsync(1) { case (next, idx) =>
            uploadChunk(bucketName, fullObjectKey, uploadId, next, idx)
              .recover {
                case e: Exception =>
                  log.error(s"Failed to upload chunk $idx", e)
                  throw e
              }
          }
          .runFold(List[CompletedPart]())(_ :+ _)
          .flatMap { parts =>
            val completeMultipartUploadRequest = makeCompleteRequest(log, bucketName, fullObjectKey, uploadId, parts)
            completeUpload(completeMultipartUploadRequest)
              .recover {
                case e: Exception =>
                  log.error(s"Failed to complete upload", e)
                  throw e
              }
          }
      }
      .recover {
        case e: Exception =>
          log.error(s"Failed to create multipart upload", e)
          throw e
      }
  }

  private def completeUpload(completeMultipartUploadRequest: CompleteMultipartUploadRequest)
                            (implicit ec: ExecutionContext): Future[CompleteMultipartUploadResponse] = {
    val eventualUploadResult = uploader.completeMultipartUpload(completeMultipartUploadRequest)

    eventualUploadResult.onComplete {
      case Success(response) => log.info(s"Finished upload to S3: $response")
      case Failure(exception) => log.error("Failed to upload to S3", exception)
    }
    eventualUploadResult
  }

  private def makeCompleteRequest(log: Logger, bucketName: String, objectKey: String, uploadId: String, parts: List[CompletedPart]) = {
    val completedMultipartUpload = CompletedMultipartUpload.builder
      .parts(parts.asJava)
      .build
    val completeMultipartUploadRequest = CompleteMultipartUploadRequest.builder
      .bucket(bucketName)
      .key(objectKey)
      .uploadId(uploadId)
      .multipartUpload(completedMultipartUpload).build

    log.info(s"Requesting completion with ${parts.length} parts")
    completeMultipartUploadRequest
  }

  private def uploadChunk(bucketName: String, objectKey: String, uploadId: String, next: Seq[ByteString], idx: Long)
                         (implicit ec: ExecutionContext): Future[CompletedPart] = {
    val partNumber = idx.toInt + 1
    val chunk = next.foldLeft(Array[Byte]())(_ ++ _.toArray)
    log.info(s"Uploading chunk $partNumber - ${chunk.length} bytes")
    val uploadPartRequest: UploadPartRequest = UploadPartRequest.builder
      .bucket(bucketName)
      .key(objectKey)
      .uploadId(uploadId)
      .partNumber(partNumber).build
    uploader.uploadPart(uploadPartRequest, AsyncRequestBody.fromBytes(chunk)).map { r =>
      CompletedPart.builder.partNumber(partNumber).eTag(r.eTag()).build()
    }
  }
}
