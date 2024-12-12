package uk.gov.homeoffice.drt.services.bx

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.dao.BorderCrossingDao
import uk.gov.homeoffice.drt.db.tables.{BorderCrossingRow, GateType}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal

import scala.concurrent.{ExecutionContext, Future}



class ImportBorderCrossingsTest extends AnyWordSpec with Matchers {
  val system: ActorSystem = ActorSystem("BxImporterTest")
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = mat.executionContext

  "BxImporter" should {
    "import the BX data" in {
      val replaceHoursForPortTerminal: (PortCode, Terminal, GateType, Iterable[BorderCrossingRow]) => Future[Unit] = {
        (portCode, terminal, gateType, rows) =>
          TestDatabase.db.run(BorderCrossingDao.replaceHours(portCode)(terminal, gateType, rows))
      }

      ImportBorderCrossings("/home/rich/Downloads/PRAU BF Data Cell - DrT Monthly Report - Oct24.xlsx", replaceHoursForPortTerminal)
    }
  }
}
