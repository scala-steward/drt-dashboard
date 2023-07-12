package uk.gov.homeoffice.drt.db

import slick.dbio.Effect
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import slick.sql.FixedSqlAction
import uk.gov.homeoffice.drt.models.RegionExport
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import java.sql.Timestamp
import scala.concurrent.ExecutionContext

class RegionExportTable(tag: Tag)
  extends Table[(String, String, String, String, String, Timestamp)](tag, "region_export") {

  def email: Rep[String] = column[String]("email")

  def region: Rep[String] = column[String]("region")

  private def startDate: Rep[String] = column[String]("start_date")

  private def endDate: Rep[String] = column[String]("end_date")

  def status: Rep[String] = column[String]("status")

  def createdAt: Rep[Timestamp] = column[java.sql.Timestamp]("created_at")

  def * = (email, region, startDate, endDate, status, createdAt)
}

object RegionExportQueries {
  private val regionExports: TableQuery[RegionExportTable] = TableQuery[RegionExportTable]

  def get(email: String, region: String, createdAt: Long)
         (implicit ec: ExecutionContext): DBIOAction[Option[RegionExport], NoStream, Effect.Read] =
    regionExports
      .filter(_.email === email)
      .filter(_.region === region)
      .filter(_.createdAt === new Timestamp(createdAt))
      .result
      .map(_.headOption.map(x => regionExportFromRow(x)))

  def getAll(email: String, region: String)
            (implicit ec: ExecutionContext): DBIOAction[Seq[RegionExport], NoStream, Effect.Read] =
    regionExports
      .filter(_.email === email)
      .filter(_.region === region)
      .result
      .map(_.map(regionExportFromRow))

  def insert(regionExport: RegionExport): DBIOAction[Int, NoStream, Effect.Write] = {
    val (startDate: String, endDate: String, createdAt: Timestamp) = dates(regionExport)
    regionExports += (regionExport.email, regionExport.region, startDate, endDate, regionExport.status, createdAt)
  }

  def update(regionExport: RegionExport): FixedSqlAction[Int, NoStream, Effect.Write] = {
    val query = for {
      export <- regionExports if export.email === regionExport.email && export.region === regionExport.region
    } yield export

    val (startDate: String, endDate: String, createdAt: Timestamp) = dates(regionExport)
    query.update(regionExport.email, regionExport.region, startDate, endDate, regionExport.status, createdAt)
  }

  private def regionExportFromRow(row: (String, String, String, String, String, Timestamp)): RegionExport = {
    val startDate = LocalDate.parse(row._3).getOrElse(throw new Exception(s"Could not parse date ${row._3}"))
    val endDate = LocalDate.parse(row._4).getOrElse(throw new Exception(s"Could not parse date ${row._4}"))
    val createdAt = SDate(row._6.getTime)
    RegionExport(row._1, row._2, startDate, endDate, row._5, createdAt)
  }

  private def dates(regionExport: RegionExport): (String, String, Timestamp) = {
    val createdAt = new Timestamp(regionExport.createdAt.millisSinceEpoch)
    (regionExport.startDate.toISOString, regionExport.endDate.toISOString, createdAt)
  }
}
