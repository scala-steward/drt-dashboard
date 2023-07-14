package uk.gov.homeoffice.drt.db

import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.wordspec.AnyWordSpec
import slick.jdbc.H2Profile.api._
import uk.gov.homeoffice.drt.models.RegionExport
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}

class RegionExportTableTest extends AnyWordSpec with BeforeAndAfter {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  val db = TestDatabase.db

  before {
    Await.ready(db.run(DBIO.seq(RegionExportQueries.regionExports.schema.dropIfExists, RegionExportQueries.regionExports.schema.create)), 1.second)
  }

  val regionExportUser1North = RegionExport("user1-email@somewhere.com", "North", LocalDate(2020, 1, 1), LocalDate(2020, 1, 2), "pending", SDate(1L))
  val regionExportUser1South = RegionExport("user1-email@somewhere.com", "South", LocalDate(2020, 1, 1), LocalDate(2020, 1, 2), "pending", SDate(2L))
  val regionExportUser2North = RegionExport("user2-email@somewhere.com", "North", LocalDate(2020, 1, 1), LocalDate(2020, 1, 2), "pending", SDate(3L))

  "RegionExportTable" should {
    "insert records and return them" in {
      val insert = RegionExportQueries.insert(regionExportUser1North)
      val get = RegionExportQueries.get(regionExportUser1North.email, regionExportUser1North.region, regionExportUser1North.createdAt.millisSinceEpoch)
      val getAll = RegionExportQueries.getAll(regionExportUser1North.email, regionExportUser1North.region)

      val result = db.run(for {
        _ <- insert
        g <- get
        ga <- getAll
      } yield (g, ga))

      assert(result.futureValue == (Some(regionExportUser1North), Seq(regionExportUser1North)))
    }
    "insert records and only return requested records" in {
      val insert1 = RegionExportQueries.insert(regionExportUser1North)
      val insert2 = RegionExportQueries.insert(regionExportUser1South)
      val insert3 = RegionExportQueries.insert(regionExportUser2North)
      val get = RegionExportQueries.get(regionExportUser1North.email, regionExportUser1North.region, regionExportUser1North.createdAt.millisSinceEpoch)
      val getAll = RegionExportQueries.getAll(regionExportUser1North.email, regionExportUser1North.region)

      val result = db.run(for {
        _ <- insert1
        _ <- insert2
        _ <- insert3
        g <- get
        ga <- getAll
      } yield (g, ga))

      assert(result.futureValue == (Some(regionExportUser1North), Seq(regionExportUser1North)))
    }
  }
}
