package uk.gov.homeoffice.drt.services.bx

import akka.actor.ActorSystem
import akka.stream.Materializer
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import slick.dbio.DBIO
import slick.jdbc.H2Profile.api._
import uk.gov.homeoffice.drt.db.TestDatabase
import uk.gov.homeoffice.drt.db.dao.BorderCrossingDao
import uk.gov.homeoffice.drt.db.serialisers.BorderCrossingSerialiser
import uk.gov.homeoffice.drt.db.tables.{BorderCrossing, GateType}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}


class ImportBorderCrossingsTest extends AnyWordSpec with Matchers with BeforeAndAfter {
  val system: ActorSystem = ActorSystem("BxImporterTest")
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContext = mat.executionContext

  before {
    val schema = BorderCrossingDao.table.schema
    Await.ready(TestDatabase.run(DBIO.seq(schema.dropIfExists, schema.create)), 1.second)
  }

  "BxImporter" should {
    "import the BX data" in {
      val replaceHoursForPortTerminal: (PortCode, Terminal, GateType, Iterable[BorderCrossing]) => Future[Int] = {
        (portCode, terminal, gateType, rows) =>
          val insert = BorderCrossingDao.replaceHours(portCode)
          TestDatabase.run(insert(terminal, gateType, rows.map(BorderCrossingSerialiser.toRow(_, SDate.now().millisSinceEpoch))))
      }

      val importFile = ImportBorderCrossings(replaceHoursForPortTerminal)

      Await.result(importFile("src/test/resources/bx-example.xlsx"), 1.second)

      val abzBx = BorderCrossingDao.totalForPortAndDate("ABZ", None)
      val bhxBx = BorderCrossingDao.totalForPortAndDate("BHX", None)
      Await.result(TestDatabase.run(abzBx(LocalDate(2024, 7, 1))), 1.second) should be(5)
      Await.result(TestDatabase.run(bhxBx(LocalDate(2024, 7, 2))), 1.second) should be(280)
    }
  }
}
