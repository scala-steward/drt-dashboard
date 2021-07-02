package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.testkit.Specs2RouteTest
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.auth.Roles.NeboUpload
import uk.gov.homeoffice.drt.routes.UploadRoutes._

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

  val test2FileData =
    """
      |Reference (URN),AssociatedText ,Flight Code ,Arrival Port ,DATE,Arrival Time,Departure Date,Departure Time,Embark Port,"Departure Port"
      |CRI/IOI/0107E/3,Passenger in transit from Costa Rica. Please refer to Operational Instructions (IOI 24-21 IOI 29 -21 IOI 48-21 IOI 60-21 IOI 66-21 IOI 73-21 IOI 92-21 IOI 99-21) for further instructions,LH0914,LHR,02/07/2021,16:40,02/07/2021,16:00,SJO,FRA
      |PHL/IOI/0107E/4,Passenger in transit from Philippines. Please refer to Operational Instructions (IOI 24-21 IOI 29 -21 IOI 48-21 IOI 60-21 IOI 66-21 IOI 73-21 IOI 92-21 IOI 99-21) for further instructions,SQ0306,LHR,02/07/2021,07:45,02/07/2021,01:10,CRK,SIN
      |PAN/IOI/0107E/12,Passenger in transit from Panama. Please refer to Operational Instructions (IOI 24-21 IOI 29 -21 IOI 48-21 IOI 60-21 IOI 66-21 IOI 73-21 IOI 92-21 IOI 99-21) for further instructions,IB3166,LHR,03/07/2021,17:05,03/07/2021,15:45,PTY,MAD
      |KEN/IOI/0107E/18,Passenger in transit from Kenya. Please refer to Operational Instructions (IOI 24-21 IOI 29 -21 IOI 48-21 IOI 60-21 IOI 66-21 IOI 73-21 IOI 92-21 IOI 99-21) for further instructions,KL1007,LHR,02/07/2021,09:00,02/07/2021,08:40,NBO,AMS
      |KEN/IOI/0107E/19,Passenger in transit from Kenya. Please refer to Operational Instructions (IOI 24-21 IOI 29 -21 IOI 48-21 IOI 60-21 IOI 66-21 IOI 73-21 IOI 92-21 IOI 99-21) for further instructions,KL1007,LHR,02/07/2021,09:00,02/07/2021,08:40,NBO,AMS
      |KEN/IOI/0107E/20,Passenger in transit from Kenya. Please refer to Operational Instructions (IOI 24-21 IOI 29 -21 IOI 48-21 IOI 60-21 IOI 66-21 IOI 73-21 IOI 92-21 IOI 99-21) for further instructions,KL1007,LHR,02/07/2021,09:00,02/07/2021,08:40,NBO,AMS
      |COL/IOI/0107E/23,Passenger in transit from Colombia. Please refer to Operational Instructions (IOI 24-21 IOI 29 -21 IOI 48-21 IOI 60-21 IOI 66-21 IOI 73-21 IOI 92-21 IOI 99-21) for further instructions,LH0922,LHR,02/07/2021,22:10,02/07/2021,21:30,BOG,FRA
      |ZAF/IOI/0107E/24,Passenger in transit from South Africa. Please refer to Operational Instructions (IOI 24-21 IOI 29 -21 IOI 48-21 IOI 60-21 IOI 66-21 IOI 73-21 IOI 92-21 IOI 99-21) for further instructions,AF1680,LHR,03/07/2021,07:55,03/07/2021,07:30,JNB,CDG
      |,,,,,,,,,
      |,,,,,,,,,
      |,,,,,,,,,
      |,,,,,,,,,
      |""".stripMargin

  val test1FileData =
    """
      |
      |CRI/IOI/0107E/3,Passenger in transit from Costa Rica.,LH0914,LHR,02/07/2021,16:40,02/07/2021,16:00,SJO,FRA
      |""".stripMargin

  val multipartForm =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict(
      "csv",
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, test1FileData),
      Map("filename" -> "test1.csv")))

  "Given a correct permission to users, the user should able to upload file successfully " >> {
    Post("/uploadFile", multipartForm) ~>
      RawHeader("X-Auth-Roles", NeboUpload.name) ~> RawHeader("X-Auth-Email", "my@email.com") ~> routes ~> check {
        responseAs[String] shouldEqual """[{"flightCount":1,"portCode":"lhr","statusCode":"202 Accepted"}]"""
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
    val flightDataF: Future[List[FlightData]] = UploadRoutes.convertByteSourceToFlightData(metaFile, Source.single(ByteString(test2FileData)))
    val exceptedResult = Seq(
      FlightData("LHR", "IB3166", covertDateTime("03/07/2021 17:05"), covertDateTime("03/07/2021 15:45"), "MAD", "PTY", 1),
      FlightData("LHR", "AF1680", covertDateTime("03/07/2021 07:55"), covertDateTime("03/07/2021 07:30"), "CDG", "JNB", 1),
      FlightData("LHR", "SQ0306", covertDateTime("02/07/2021 07:45"), covertDateTime("02/07/2021 01:10"), "SIN", "CRK", 1),
      FlightData("LHR", "LH0914", covertDateTime("02/07/2021 16:40"), covertDateTime("02/07/2021 16:00"), "FRA", "SJO", 1),
      FlightData("LHR", "LH0922", covertDateTime("02/07/2021 22:10"), covertDateTime("02/07/2021 21:30"), "FRA", "BOG", 1),
      FlightData("LHR", "KL1007", covertDateTime("02/07/2021 09:00"), covertDateTime("02/07/2021 08:40"), "AMS", "NBO", 3))

    val flightDataResult: Seq[FlightData] = Await.result(flightDataF, 1 seconds)

    flightDataResult must containAllOf(exceptedResult)
  }

  "covertDateTime should convert String date format to millis as expected" >> {
    val date = "03/07/2021 17:05"
    val millisDate = 1625328300000L
    millisDate mustEqual covertDateTime(date)
  }
}

