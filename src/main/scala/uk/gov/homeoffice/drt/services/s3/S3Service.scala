package uk.gov.homeoffice.drt.services.s3

import akka.stream.Materializer
import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import uk.gov.homeoffice.drt.ServerConfig

import scala.concurrent.ExecutionContext

object S3Service {

  private def s3ClientBuilder(serverConfig: ServerConfig): S3AsyncClient = {
    val credentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(serverConfig.s3AccessKey, serverConfig.s3SecretAccessKey))

    S3AsyncClient.builder()
      .region(Region.EU_WEST_2)
      .credentialsProvider(credentialsProvider)
      .build()
  }

  def s3FileUploaderAndDownloader(serverConfig: ServerConfig, folderPrefix: String)(implicit ec: ExecutionContext, mat: Materializer): (S3Uploader, S3Downloader) = {

    val s3Client: S3AsyncClient = s3ClientBuilder(serverConfig)

    val multipartUploader = ProdS3MultipartUploader(s3Client)
    val uploader = S3Uploader(multipartUploader, serverConfig.drtS3BucketName, Option(folderPrefix))
    val downloader = S3Downloader(s3Client, serverConfig.drtS3BucketName)
    (uploader, downloader)
  }


}
