package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.testkit.Specs2RouteTest
import com.github.tototoshi.csv.{ CSVFormat, QUOTE_MINIMAL, Quoting }
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.auth.Roles.NeboUpload

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ Await, ExecutionContextExecutor, Future }

class NeboUploadRoutesSpec extends Specification with Specs2RouteTest {

  val testKit: ActorTestKit = ActorTestKit()

  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContextExecutor = sys.executionContext

  val httpResponse = HttpResponse(StatusCodes.Accepted, entity = HttpEntity("File uploaded"))

  val mockHttpClient = new MockHttpClient(httpResponse)

  private val neboRoutes: NeboUploadRoutes = NeboUploadRoutes(List("lhr"), mockHttpClient)

  val test2FileData: String =
    """Reference (URN),AssociatedText ,Flight Code ,Arrival Port ,DATE,Arrival Time,Departure Date,Departure Time,Embark Port,"Departure Port"
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

  val test1FileData: String =
    """Reference (URN),AssociatedText ,Flight Code ,Arrival Port ,DATE,Arrival Time
      |CRI/IOI/0107E/3,Passenger in transit from testCountry1.,TEST914,LHR,02/07/2021,16:40,02/07/2021,16:00,SJO,FRA
      |""".stripMargin

  val test3FileDataWithoutDepartureDetails: String =
    """Reference (URN),AssociatedText ,Flight Code ,Arrival Port ,DATE,Arrival Time,Departure Date,Departure Time,Embark Port,"Departure Port"
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

  val test4FileDataWithNewlineCharInFields: String =
    """Reference (URN),AssociatedText ,"Flight
      |Code ","Arrival
      |Port ","Date","Arrival
      |time",Departure Date,Departure Time,Embark Port,"Departure
      |Port"
      |MDV/IOI/2308L/7,Passenger in transit from testCountry7.,TEST124,LHR,24/08/2021,06:15,24/08/2021,01:00,MLE,BAH
      |PAK/IOI/2308L/8,Passenger in transit from testCountry8.,TEST007,LHR,24/08/2021,06:55,24/08/2021,02:00,SKT,BAH
      |PAK/IOI/2308L/9,Passenger in transit from testCountry8.,TEST007,LHR,24/08/2021,06:55,24/08/2021,02:00,SKT,BAH
      |PAK/IOI/2308L/10,Passenger in transit from testCountry8.,TEST007,LHR,24/08/2021,06:55,24/08/2021,02:00,SKT,BAH
      |PAK/IOI/2308L/11,Passenger in transit from testCountry8.,TEST007,LHR,24/08/2021,06:55,24/08/2021,02:00,SKT,BAH
      |""".stripMargin

  val testFile: String =
    """Reference (URN),AssociatedText ,Flight Code ,Arrival Port ,DATE,Document Number,Arrival Time,Departure Date,Departure Time,Embark Port,"Departure
      |Port"
      |PHL/IOI/2309L/125,Passenger in transit from Philippines. Please refer to Operational Instructions (IOI 134-21) for further instructions,AA1234,LHR,01/08/2021,ABCDEF123,16:50,01/08/2021,16:10,AAA,BBB
      |PHL/IOI/2309L/126,Passenger in transit from Philippines. Please refer to Operational Instructions (IOI 134-21) for further instructions,AA1234,LHR,22/08/2021,ABCDEF123,16:40,22/08/2021,16:10,AAA,BBB
      |""".stripMargin

  val multipartForm: FormData.Strict =
    Multipart.FormData(Multipart.FormData.BodyPart.Strict(
      "csv",
      HttpEntity(ContentTypes.`text/plain(UTF-8)`, test1FileData),
      Map("filename" -> "test1.csv")))

  "Given a correct permission to users, the user should able to upload file successfully " >> {
    Post("/nebo-upload", multipartForm) ~>
      RawHeader("X-Auth-Roles", NeboUpload.name) ~> RawHeader("X-Auth-Email", "my@email.com") ~> neboRoutes.route ~> check {
        responseAs[String] shouldEqual """[{"flightCount":1,"portCode":"lhr","statusCode":"202 Accepted"}]"""
      }
  }

  "Given a incorrect permission to users, the user is forbidden to upload" >> {
    Post("/nebo-upload", multipartForm) ~>
      RawHeader("X-Auth-Roles", "random") ~> RawHeader("X-Auth-Email", "my@email.com") ~> Route.seal(neboRoutes.route) ~>
      check {
        status shouldEqual StatusCodes.Forbidden
        responseAs[String] shouldEqual """You are not authorized to upload!"""
      }
  }

  "convertByteSourceToFlightData should convert file data byteString to FlightData case class with expected conversion" >> {
    val metaFile = FileInfo(fieldName = "csv", fileName = "test.csv", contentType = ContentTypes.`text/plain(UTF-8)`)
    val flightDataF: Future[List[FlightData]] = neboRoutes.convertByteSourceToFlightData(metaFile, Future.successful(test2FileData))
    val exceptedResult = Seq(
      FlightData("LHR", "TEST316", neboRoutes.parseDateToMillis("03/07/2021 17:05"), Option(neboRoutes.parseDateToMillis("03/07/2021 15:45")), Option("MAD"), Option("PTY"), Seq("PAN/IOI/0107E/12")),
      FlightData("LHR", "TEST1681", neboRoutes.parseDateToMillis("03/07/2021 07:55"), Option(neboRoutes.parseDateToMillis("03/07/2021 07:30")), Option("CDG"), Option("JNB"), Seq("ZAF/IOI/0107E/24")),
      FlightData("LHR", "TEST306", neboRoutes.parseDateToMillis("02/07/2021 07:45"), Option(neboRoutes.parseDateToMillis("02/07/2021 01:10")), Option("SIN"), Option("CRK"), Seq("PHL/IOI/0107E/4")),
      FlightData("LHR", "TEST914", neboRoutes.parseDateToMillis("02/07/2021 16:40"), Option(neboRoutes.parseDateToMillis("02/07/2021 16:00")), Option("FRA"), Option("SJO"), Seq("CRI/IOI/0107E/3")),
      FlightData("LHR", "TEST922", neboRoutes.parseDateToMillis("02/07/2021 22:10"), Option(neboRoutes.parseDateToMillis("02/07/2021 21:30")), Option("FRA"), Option("BOG"), Seq("COL/IOI/0107E/23")),
      FlightData("LHR", "TEST1007", neboRoutes.parseDateToMillis("02/07/2021 09:00"), Option(neboRoutes.parseDateToMillis("02/07/2021 08:40")), Option("AMS"), Option("NBO"), Seq("KEN/IOI/0107E/18", "KEN/IOI/0107E/19")),
      FlightData("LHR", "TEST1007", neboRoutes.parseDateToMillis("03/07/2021 09:00"), Option(neboRoutes.parseDateToMillis("02/07/2021 08:40")), Option("AMS"), Option("NBO"), Seq("KEN/IOI/0107E/20")))
    val flightDataResult: Seq[FlightData] = Await.result(flightDataF, 1.seconds)

    flightDataResult must containAllOf(exceptedResult)
  }

  "convertByteSourceToFlightData should convert file data byteString to FlightData case class with expected conversion without departure details" >> {
    val metaFile = FileInfo(fieldName = "csv", fileName = "test.csv", contentType = ContentTypes.`text/plain(UTF-8)`)
    val flightDataF: Future[List[FlightData]] = neboRoutes.convertByteSourceToFlightData(metaFile, Future.successful(test3FileDataWithoutDepartureDetails))
    val exceptedResult = Seq(
      FlightData("LHR", "TEST316", neboRoutes.parseDateToMillis("03/07/2021 17:05"), None, None, None, Seq("PAN/IOI/0107E/12")),
      FlightData("LHR", "TEST1681", neboRoutes.parseDateToMillis("03/07/2021 07:55"), None, None, None, Seq("ZAF/IOI/0107E/24")),
      FlightData("LHR", "TEST306", neboRoutes.parseDateToMillis("02/07/2021 07:45"), None, None, None, Seq("PHL/IOI/0107E/4")),
      FlightData("LHR", "TEST914", neboRoutes.parseDateToMillis("02/07/2021 16:40"), None, None, None, Seq("CRI/IOI/0107E/3")),
      FlightData("LHR", "TEST922", neboRoutes.parseDateToMillis("02/07/2021 22:10"), None, None, None, Seq("COL/IOI/0107E/23")),
      FlightData("LHR", "TEST1007", neboRoutes.parseDateToMillis("02/07/2021 09:00"), None, None, None, Seq("KEN/IOI/0107E/18", "KEN/IOI/0107E/19")),
      FlightData("LHR", "TEST1007", neboRoutes.parseDateToMillis("03/07/2021 09:00"), None, None, None, Seq("KEN/IOI/0107E/20")))
    val flightDataResult: Seq[FlightData] = Await.result(flightDataF, 1.seconds)

    flightDataResult must containAllOf(exceptedResult)
  }

  "convertByteSourceToFlightData should convert file data to FlightData while field data contain newline character" >> {
    val metaFile = FileInfo(fieldName = "csv", fileName = "test.csv", contentType = ContentTypes.`text/plain(UTF-8)`)
    val flightDataF: Future[List[FlightData]] = neboRoutes.convertByteSourceToFlightData(metaFile, Future.successful(test4FileDataWithNewlineCharInFields))
    val exceptedResult = Seq(
      FlightData("LHR", "TEST124", neboRoutes.parseDateToMillis("24/08/2021 06:15"), Option(neboRoutes.parseDateToMillis("24/08/2021 01:00")), Some("BAH"), Some("MLE"), Seq("MDV/IOI/2308L/7")),
      FlightData("LHR", "TEST007", neboRoutes.parseDateToMillis("24/08/2021 06:55"), Option(neboRoutes.parseDateToMillis("24/08/2021 02:00")), Some("BAH"), Some("SKT"), Seq(
        "PAK/IOI/2308L/8",
        "PAK/IOI/2308L/9",
        "PAK/IOI/2308L/10",
        "PAK/IOI/2308L/11")))
    val flightDataResult: Seq[FlightData] = Await.result(flightDataF, 1 seconds)

    flightDataResult must containAllOf(exceptedResult)
  }

  "covertDateTime should convert String date format to millis as expected" >> {
    val date = "03/07/2021 17:05"
    val millisDate = 1625328300000L
    millisDate mustEqual neboRoutes.parseDateToMillis(date)
  }

  "convertByteSourceToFlightData should convert data containing additional columns" >> {
    val metaFile = FileInfo(fieldName = "csv", fileName = "test.csv", contentType = ContentTypes.`text/plain(UTF-8)`)
    val flightDataF: Future[List[FlightData]] = neboRoutes.convertByteSourceToFlightData(metaFile, Future.successful(testFile))
    val exceptedResult = Set(
      FlightData("LHR", "AA1234", 1627833000000L, Some(1627830600000L), Some("BBB"), Some("AAA"), Seq("PHL/IOI/2309L/125")),
      FlightData("LHR", "AA1234", 1629646800000L, Some(1629645000000L), Some("BBB"), Some("AAA"), Seq("PHL/IOI/2309L/126")))
    val flightDataResult: Set[FlightData] = Await.result(flightDataF, 1.seconds).toSet

    flightDataResult === exceptedResult
  }

  object CsvFormat extends CSVFormat {
    override val delimiter: Char = ','
    override val quoteChar: Char = '"'
    override val escapeChar: Char = '\\'
    override val lineTerminator: String = "\n"
    override val quoting: Quoting = QUOTE_MINIMAL
    override val treatEmptyLineAsNil: Boolean = true
  }
}

