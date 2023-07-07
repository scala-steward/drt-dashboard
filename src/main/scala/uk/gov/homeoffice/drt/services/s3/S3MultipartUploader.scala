package uk.gov.homeoffice.drt.services.s3

import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model._

import scala.concurrent.Future
import scala.jdk.FutureConverters._


trait S3MultipartUploader {
  val createMultipartUpload: CreateMultipartUploadRequest => Future[CreateMultipartUploadResponse]
  val completeMultipartUpload: CompleteMultipartUploadRequest => Future[CompleteMultipartUploadResponse]
  val uploadPart: (UploadPartRequest, AsyncRequestBody) => Future[UploadPartResponse]
}

case class ProdS3MultipartUploader(s3Client: S3AsyncClient) extends S3MultipartUploader {
  override val createMultipartUpload: CreateMultipartUploadRequest => Future[CreateMultipartUploadResponse] =
    request => s3Client.createMultipartUpload(request).asScala
  override val completeMultipartUpload: CompleteMultipartUploadRequest => Future[CompleteMultipartUploadResponse] =
    request => s3Client.completeMultipartUpload(request).asScala
  override val uploadPart: (UploadPartRequest, AsyncRequestBody) => Future[UploadPartResponse] =
    (request, body) => s3Client.uploadPart(request, body).asScala
}
