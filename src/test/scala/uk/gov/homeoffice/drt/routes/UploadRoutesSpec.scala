package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.testkit.Specs2RouteTest
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.auth.Roles.NeboUpload

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }

class UploadRoutesSpec extends Specification with Specs2RouteTest {

  val testKit = ActorTestKit()

  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val ec = sys.executionContext

  val httpClient = new HttpClient {
    override def send(httpRequest: HttpRequest)(implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[HttpResponse] =
      Future(HttpResponse(StatusCodes.Accepted, entity = HttpEntity("File uploaded")))(ec)
  }

  val routes: Route = UploadRoutes(
    "uploadFile",
    List("lhr"), httpClient)

  val fileData =
    """
      |AGO/IOI/3004E/2,Passenger in transit from Angola.,KL1234,EDI,30/04/2021 10:15:00
      |ZAF/IOI/3004E/3,Passenger in transit from South Africa.,TK1234,LHR,01/05/2021 09:50:00
      |ZAF/IOI/3004E/4,Passenger in transit from South Africa.,LH4321,LHR,01/05/2021 08:40:00
      |ZAF/IOI/3004E/5,Passenger in transit from South Africa.,LH4321,LHR,01/05/2021 08:40:00
      |ZAF/IOI/3004E/6,Passenger in transit from South Africa.,LH4321,LHR,01/05/2021 08:40:00
      |ZAF/IOI/3004E/7,Passenger in transit from South Africa.,LH4321,LHR,01/05/2021 08:40:00
      |ZAF/IOI/3004E/8,Passenger in transit from South Africa.,KL4321,LHR,01/05/2021 16:15:00
      |ZAF/IOI/3004E/9,Passenger in transit from South Africa.,KL4321,LHR,01/05/2021 16:15:00
      |,,,,
      |,,,,
      |,,,,
      |,,,,
      |""".stripMargin

  val multipartForm =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict(
      "csv",
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, fileData),
      Map("filename" -> "primes.csv")))

  "Given a correct permission to users, the user should able to upload file successfully " >> {
    Post("/uploadFile", multipartForm) ~>
      RawHeader("X-Auth-Roles", NeboUpload.name) ~> RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
        responseAs[String] shouldEqual """[{"flightCount":3,"portCode":"lhr","statusCode":"202 Accepted"}]"""
      }
  }

  "Given a incorrect permission to users, the user is forbidden to upload" >> {
    Post("/uploadFile", multipartForm) ~>
      RawHeader("X-Auth-Roles", "random") ~> RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[String] shouldEqual """You are not authorized to upload!"""
      }
  }

  "convertByteSourceToFlightData should convert file data byteString to FlightData case class with expected conversion" >> {
    val metaFile = FileInfo(fieldName = "csv", fileName = "test.csv", contentType = ContentTypes.`text/plain(UTF-8)`)
    val flightDataF: Future[List[FlightData]] = UploadRoutes.convertByteSourceToFlightData(metaFile, Source.single(ByteString(fileData)))
    val exceptedResult = Seq(
      FlightData("EDI", "KL1234", UploadRoutes.covertDateTime("30/04/2021 10:15:00"), 1),
      FlightData("LHR", "TK1234", UploadRoutes.covertDateTime("01/05/2021 09:50:00"), 1),
      FlightData("LHR", "LH4321", UploadRoutes.covertDateTime("01/05/2021 08:40:00"), 4),
      FlightData("LHR", "KL4321", UploadRoutes.covertDateTime("01/05/2021 16:15:00"), 2))
    val flightDataResult: Seq[FlightData] = Await.result(flightDataF, 1 seconds)

    flightDataResult must containAllOf(exceptedResult)
  }
}

