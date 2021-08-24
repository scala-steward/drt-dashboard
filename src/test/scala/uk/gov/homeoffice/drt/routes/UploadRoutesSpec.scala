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
      |CRI/IOI/0107E/3,Passenger in transit from testCountry1.,TEST914,LHR,02/07/2021,16:40,02/07/2021,16:00,SJO,FRA
      |PHL/IOI/0107E/4,Passenger in transit from testCountry2.,TEST306,LHR,02/07/2021,07:45,02/07/2021,01:10,CRK,SIN
      |PAN/IOI/0107E/12,Passenger in transit from testCountry3.,TEST316,LHR,03/07/2021,17:05,03/07/2021,15:45,PTY,MAD
      |KEN/IOI/0107E/18,Passenger in transit from testCountry4.,TEST1007,LHR,02/07/2021,09:00,02/07/2021,08:40,NBO,AMS
      |KEN/IOI/0107E/19,Passenger in transit from testCountry4.,TEST1007,LHR,02/07/2021,09:00,02/07/2021,08:40,NBO,AMS
      |KEN/IOI/0107E/20,Passenger in transit from testCountry4.,TEST1007,LHR,03/07/2021,09:00,02/07/2021,08:40,NBO,AMS
      |COL/IOI/0107E/23,Passenger in transit from testCountry5.,TEST922,LHR,02/07/2021,22:10,02/07/2021,21:30,BOG,FRA
      |ZAF/IOI/0107E/24,Passenger in transit from testCountry6.,TEST1681,LHR,03/07/2021,07:55,03/07/2021,07:30,JNB,CDG
      |,,,,,,,,,
      |,,,,,,,,,
      |,,,,,,,,,
      |,,,,,,,,,
      |""".stripMargin

  val test1FileData =
    """
      |
      |CRI/IOI/0107E/3,Passenger in transit from testCountry1.,TEST914,LHR,02/07/2021,16:40,02/07/2021,16:00,SJO,FRA
      |""".stripMargin

  val test3FileDataWithoutDepartureDetails =
    """
      |Reference (URN),AssociatedText ,Flight Code ,Arrival Port ,DATE,Arrival Time,Departure Date,Departure Time,Embark Port,"Departure Port"
      |CRI/IOI/0107E/3,Passenger in transit from testCountry1.,TEST914,LHR,02/07/2021,16:40
      |PHL/IOI/0107E/4,Passenger in transit from testCountry2.,TEST306,LHR,02/07/2021,07:45
      |PAN/IOI/0107E/12,Passenger in transit from testCountry3.,TEST316,LHR,03/07/2021,17:05
      |KEN/IOI/0107E/18,Passenger in transit from testCountry4.,TEST1007,LHR,02/07/2021,09:00
      |KEN/IOI/0107E/19,Passenger in transit from testCountry4.,TEST1007,LHR,02/07/2021,09:00
      |KEN/IOI/0107E/20,Passenger in transit from testCountry4.,TEST1007,LHR,03/07/2021,09:00
      |COL/IOI/0107E/23,Passenger in transit from testCountry5.,TEST922,LHR,02/07/2021,22:10
      |ZAF/IOI/0107E/24,Passenger in transit from testCountry6.,TEST1681,LHR,03/07/2021,07:55
      |,,,,,,,,,
      |,,,,,,,,,
      |,,,,,,,,,
      |,,,,,,,,,
      |""".stripMargin

  val test4FileDataWithNewlineCharInFields =
    """
      |Reference (URN),AssociatedText ,"Flight
      |Code ","Arrival
      |Port ","Arrival
      |Date","Arrival
      |time",Departure Date,Departure Time,Embark Port,"Departure
      |Port"
      |MDV/IOI/2308L/7,Passenger in transit from testCountry7.,TEST124,LHR,24/08/2021,06:15,24/08/2021,01:00,MLE,BAH
      |PAK/IOI/2308L/8,Passenger in transit from testCountry8.,TEST007,LHR,24/08/2021,06:55,24/08/2021,02:00,SKT,BAH
      |PAK/IOI/2308L/9,Passenger in transit from testCountry8.,TEST007,LHR,24/08/2021,06:55,24/08/2021,02:00,SKT,BAH
      |PAK/IOI/2308L/10,Passenger in transit from testCountry8.,TEST007,LHR,24/08/2021,06:55,24/08/2021,02:00,SKT,BAH
      |PAK/IOI/2308L/11,Passenger in transit from testCountry8.,TEST007,LHR,24/08/2021,06:55,24/08/2021,02:00,SKT,BAH
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
      FlightData("LHR", "TEST316", parseDateToMillis("03/07/2021 17:05"), Option(parseDateToMillis("03/07/2021 15:45")), Option("MAD"), Option("PTY"), 1),
      FlightData("LHR", "TEST1681", parseDateToMillis("03/07/2021 07:55"), Option(parseDateToMillis("03/07/2021 07:30")), Option("CDG"), Option("JNB"), 1),
      FlightData("LHR", "TEST306", parseDateToMillis("02/07/2021 07:45"), Option(parseDateToMillis("02/07/2021 01:10")), Option("SIN"), Option("CRK"), 1),
      FlightData("LHR", "TEST914", parseDateToMillis("02/07/2021 16:40"), Option(parseDateToMillis("02/07/2021 16:00")), Option("FRA"), Option("SJO"), 1),
      FlightData("LHR", "TEST922", parseDateToMillis("02/07/2021 22:10"), Option(parseDateToMillis("02/07/2021 21:30")), Option("FRA"), Option("BOG"), 1),
      FlightData("LHR", "TEST1007", parseDateToMillis("02/07/2021 09:00"), Option(parseDateToMillis("02/07/2021 08:40")), Option("AMS"), Option("NBO"), 2),
      FlightData("LHR", "TEST1007", parseDateToMillis("03/07/2021 09:00"), Option(parseDateToMillis("02/07/2021 08:40")), Option("AMS"), Option("NBO"), 1))
    val flightDataResult: Seq[FlightData] = Await.result(flightDataF, 1 seconds)

    flightDataResult must containAllOf(exceptedResult)
  }

  "convertByteSourceToFlightData should convert file data byteString to FlightData case class with expected conversion without departure details" >> {
    val metaFile = FileInfo(fieldName = "csv", fileName = "test.csv", contentType = ContentTypes.`text/plain(UTF-8)`)
    val flightDataF: Future[List[FlightData]] = UploadRoutes.convertByteSourceToFlightData(metaFile, Source.single(ByteString(test3FileDataWithoutDepartureDetails)))
    val exceptedResult = Seq(
      FlightData("LHR", "TEST316", parseDateToMillis("03/07/2021 17:05"), None, None, None, 1),
      FlightData("LHR", "TEST1681", parseDateToMillis("03/07/2021 07:55"), None, None, None, 1),
      FlightData("LHR", "TEST306", parseDateToMillis("02/07/2021 07:45"), None, None, None, 1),
      FlightData("LHR", "TEST914", parseDateToMillis("02/07/2021 16:40"), None, None, None, 1),
      FlightData("LHR", "TEST922", parseDateToMillis("02/07/2021 22:10"), None, None, None, 1),
      FlightData("LHR", "TEST1007", parseDateToMillis("02/07/2021 09:00"), None, None, None, 2),
      FlightData("LHR", "TEST1007", parseDateToMillis("03/07/2021 09:00"), None, None, None, 1))
    val flightDataResult: Seq[FlightData] = Await.result(flightDataF, 1 seconds)

    flightDataResult must containAllOf(exceptedResult)
  }

  "convertByteSourceToFlightData should convert file data to FlightData while field data contain newline character" >> {
    val metaFile = FileInfo(fieldName = "csv", fileName = "test.csv", contentType = ContentTypes.`text/plain(UTF-8)`)
    val flightDataF: Future[List[FlightData]] = UploadRoutes.convertByteSourceToFlightData(metaFile, Source.single(ByteString(test4FileDataWithNewlineCharInFields)))
    val exceptedResult = Seq(
      FlightData("LHR", "TEST124", parseDateToMillis("24/08/2021 06:15"), Option(parseDateToMillis("24/08/2021 01:00")), Some("BAH"), Some("MLE"), 1),
      FlightData("LHR", "TEST007", parseDateToMillis("24/08/2021 06:55"), Option(parseDateToMillis("24/08/2021 02:00")), Some("BAH"), Some("SKT"), 4))
    val flightDataResult: Seq[FlightData] = Await.result(flightDataF, 1 seconds)

    flightDataResult must containAllOf(exceptedResult)
  }

  "covertDateTime should convert String date format to millis as expected" >> {
    val date = "03/07/2021 17:05"
    val millisDate = 1625328300000L
    millisDate mustEqual parseDateToMillis(date)
  }

}

