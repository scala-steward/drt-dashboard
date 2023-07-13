package uk.gov.homeoffice.drt.services.s3

import akka.stream.IOResult
import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, GetObjectResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.CompletionStageOps

case class S3Downloader(s3Client: S3AsyncClient, bucketName: String)
                       (implicit ec: ExecutionContext) {
  val download: String => Future[Source[ByteString, _]] =
    objectKey => {
      val request = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build()

      s3Client
        .getObject(request, AsyncResponseTransformer.toBytes[GetObjectResponse]).asScala
        .map(response => StreamConverters.fromInputStream(() => response.asInputStream()))
    }
}
