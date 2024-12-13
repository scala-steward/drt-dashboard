package uk.gov.homeoffice.drt.routes

import akka.Done
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.testkit.TestProbe
import akka.util.ByteString
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.db.serialisers.BorderCrossingSerialiser
import uk.gov.homeoffice.drt.db.tables._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2, Terminal}
import uk.gov.homeoffice.drt.services.bx.ImportBorderCrossings
import uk.gov.homeoffice.drt.time.UtcDate

import java.io.File
import java.nio.file.{Files, Paths}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object BorderCrossingRoutes {
  private val log = LoggerFactory.getLogger(getClass)

  def tempDestination(fileInfo: FileInfo): File =
    Files.createTempFile(fileInfo.fileName, ".tmp").toFile

  def apply(replaceHoursForPortTerminal: (PortCode, Terminal, GateType, Iterable[BorderCrossingRow]) => Future[Int])
           (implicit mat: Materializer, ec: ExecutionContext): Route = {

    val importFile: String => Future[Int] = ImportBorderCrossings(replaceHoursForPortTerminal)

    pathPrefix("border-crossing") {
      storeUploadedFile("csv", tempDestination) {
        case (_, file) =>
          val eventualDone = importFile(file.getPath)

          onComplete(eventualDone) {
            case Success(_) => complete(StatusCodes.OK)
            case Failure(error) =>
              log.error(s"Error importing border crossings: ${error.getMessage}")
              complete(StatusCodes.InternalServerError)
          }
      }
    }
  }
}

class BorderCrossingRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)

  val probeRow: TestProbe = TestProbe("rows")

  val fileContent: ByteString = ByteString(Files.readAllBytes(Paths.get("src/test/resources/bx-example.xlsx")))

  val replaceHoursForPortTerminal: (PortCode, Terminal, GateType, Iterable[BorderCrossingRow]) => Future[Int] =
    (pc, t, gt, rows) => {
      probeRow.ref ! (pc, t, gt, rows)
      Future.successful(rows.size)
    }

  def tempDestination(fileInfo: FileInfo): File =
    Files.createTempFile(fileInfo.fileName, ".tmp").toFile

  val multipartForm: FormData.Strict =
    Multipart.FormData(
      Multipart.FormData.BodyPart.Strict(
        "csv",
        HttpEntity(ContentTypes.`application/octet-stream`, fileContent),
        Map("filename" -> "bx.xlsx")))

  "BorderCrossingRoutes" should {
    "upload a file" in {
      Post("/border-crossing", multipartForm) ~> BorderCrossingRoutes(replaceHoursForPortTerminal) ~> check {
        List(
          BorderCrossing(PortCode("ABZ"), T1, UtcDate(2024, 7, 1), Pcp, 0, 5),
          BorderCrossing(PortCode("BHX"), T2, UtcDate(2024, 7, 2), EGate, 10, 280),
        )
        .map(checkRow)
        status shouldEqual StatusCodes.OK
      }
    }
  }

  private def checkRow(crossing: BorderCrossing) = {
    probeRow.fishForMessage(1.second) {
      case (pc: PortCode, t: Terminal, gt: GateType, rows: List[BorderCrossingRow]) =>
        val row = rows.head
        val actualRow = BorderCrossingSerialiser.fromRow(row)

        pc == crossing.portCode && t == crossing.terminal && gt == crossing.gateType && actualRow == crossing
    }
  }
}
