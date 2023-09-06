package uk.gov.homeoffice.drt.routes

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString
import akka.{Done, NotUsed}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._
import uk.gov.homeoffice.drt.MockHttpClient
import uk.gov.homeoffice.drt.arrivals.ArrivalExportHeadings
import uk.gov.homeoffice.drt.db.{AppDatabase, TestDatabase}
import uk.gov.homeoffice.drt.exports.{Arrivals, ExportPort}
import uk.gov.homeoffice.drt.json.ExportJsonFormats.exportRequestJsonFormat
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor, Future}


class ExportRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest with BeforeAndAfter {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  implicit val csvStreaming: CsvEntityStreamingSupport = EntityStreamingSupport.csv()

  lazy val db: Database = Database.forConfig("h2-db")

  before {
    val schema = TestDatabase.regionExportTable.schema
    Await.ready(db.run(DBIO.seq(schema.dropIfExists, schema.create)), 1.second)
  }

  val csv: String =
    """flight1,information,row
      |flight2,information,row
      |""".stripMargin

  val mockHttpClient: MockHttpClient = MockHttpClient(() => csv)
  val uploadProbe: TestProbe[(String, String)] = TestProbe[(String, String)]()
  val downloadProbe: TestProbe[String] = TestProbe[String]()
  val mockUploader: (String, Source[ByteString, Any]) => Future[Done.type] = (objectKey: String, data: Source[ByteString, Any]) =>
    data
      .runReduce[ByteString](_ ++ _).map { bytes =>
      uploadProbe.ref ! (objectKey, bytes.utf8String)
      Done
    }
  val mockDownloader: String => Future[Source[ByteString, NotUsed]] = (objectKey: String) => {
    downloadProbe.ref ! objectKey
    Future.successful(Source(Seq(ByteString("1"), ByteString("2"), ByteString("3"))))
  }
  val now: SDateLike = SDate("2022-08-02T00:00:00")
  val nowYYYYMMDDHHmmss = "20220802000000"
  val nowProvider: () => SDateLike = () => now

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  implicit val testDb: AppDatabase = TestDatabase

  "Request heathrow arrival export" should {
    "collate all requested terminal arrivals" in {
      val exportPorts = Seq(ExportPort("lhr", Seq("t2", "t5")))
      val request = LegacyExportRoutes.ExportRequest(Arrivals, exportPorts, LocalDate(2022, 8, 2), LocalDate(2022, 8, 3))
      Post("/export", request) ~> RawHeader("X-Auth-Email", "someone@somwehere.com") ~> LegacyExportRoutes(mockHttpClient, mockUploader, mockDownloader, nowProvider) ~> check {
        uploadProbe.expectMessage((s"lhr-$nowYYYYMMDDHHmmss-2022-08-02-to-2022-08-03.csv", heathrowRegionPortTerminalData))
        responseAs[String] should ===("ok")
      }
    }
  }

  def heathrowRegionPortTerminalData: String =
    s"""${ArrivalExportHeadings.regionalExportHeadings}
       |flight1,information,row
       |flight2,information,row
       |flight1,information,row
       |flight2,information,row
       |""".stripMargin
}
