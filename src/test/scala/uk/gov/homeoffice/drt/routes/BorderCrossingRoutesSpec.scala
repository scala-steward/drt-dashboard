package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.Multipart.FormData
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, Multipart, StatusCodes}
import akka.http.scaladsl.server.AuthorizationFailedRejection
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import akka.testkit.TestProbe
import akka.util.ByteString
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.tables._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2, Terminal}
import uk.gov.homeoffice.drt.time.UtcDate

import java.io.File
import java.nio.file.{Files, Paths}
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt


class BorderCrossingRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)

  val probeRow: TestProbe = TestProbe("rows")

  val fileContent: ByteString = ByteString(Files.readAllBytes(Paths.get("src/test/resources/bx-example.xlsx")))

  val replaceHoursForPortTerminal: (PortCode, Terminal, GateType, Iterable[BorderCrossing]) => Future[Int] =
    (pc, t, gt, rows) => {
      probeRow.ref ! (pc, t, gt, rows)
      Future.successful(rows.size)
    }

  def tempDestination(fileInfo: FileInfo): File =
    Files.createTempFile(fileInfo.fileName, ".tmp").toFile

  val multipartForm: FormData.Strict =
    Multipart.FormData(
      Multipart.FormData.BodyPart.Strict(
        "excel",
        HttpEntity(ContentTypes.`application/octet-stream`, fileContent),
        Map("filename" -> "bx.xlsx")))

  "BorderCrossingRoutes" should {
    "upload a file" in {
      Post("/border-crossing", multipartForm) ~>
        RawHeader("X-Forwarded-Groups", "manage-users") ~>
        RawHeader("X-Forwarded-Email", "my@email.com") ~>
        BorderCrossingRoutes(replaceHoursForPortTerminal) ~>
        check {
          List(
            BorderCrossing(PortCode("ABZ"), T1, UtcDate(2024, 7, 1), Pcp, 0, 5),
            BorderCrossing(PortCode("BHX"), T2, UtcDate(2024, 7, 2), EGate, 10, 280),
          )
            .map(checkRow)
          status shouldEqual StatusCodes.OK
        }
    }
    "reject a request without the ManageUsers role" in {
      Post("/border-crossing", multipartForm) ~>
        RawHeader("X-Forwarded-Groups", "") ~>
        RawHeader("X-Forwarded-Email", ")") ~>
        BorderCrossingRoutes(replaceHoursForPortTerminal) ~>
        check {
          rejection should ===(AuthorizationFailedRejection)
        }
    }
  }

  private def checkRow(crossing: BorderCrossing) = {
    probeRow.fishForMessage(2.second) {
      case (pc: PortCode, t: Terminal, gt: GateType, rows: List[BorderCrossing]) =>
        val row = rows.head

        pc == crossing.portCode && t == crossing.terminal && gt == crossing.gateType && row == crossing
    }
  }
}
