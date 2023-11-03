package uk.gov.homeoffice.drt.db

import slick.dbio.Effect
import slick.lifted.Tag
import slick.sql.FixedSqlAction
import uk.gov.homeoffice.drt.db.Db.slickProfile.api._
import uk.gov.homeoffice.drt.models.Export
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import java.sql.Timestamp
import scala.concurrent.ExecutionContext

class ExportTable(tag: Tag)
  extends Table[(String, String, String, String, String, Timestamp)](tag, "export") {

  def email: Rep[String] = column[String]("email")

  def terminals: Rep[String] = column[String]("terminals")

  private def startDate: Rep[String] = column[String]("start_date")

  private def endDate: Rep[String] = column[String]("end_date")

  def status: Rep[String] = column[String]("status")

  def createdAt: Rep[Timestamp] = column[java.sql.Timestamp]("created_at")

  def pk = primaryKey("pk_region_export_created", (email, terminals, createdAt))

  def * = (email, terminals, startDate, endDate, status, createdAt)
}

object ExportQueries {
  val regionExports: TableQuery[ExportTable] = TableQuery[ExportTable]

  def get(email: String, createdAt: Long)
         (implicit ec: ExecutionContext): DBIOAction[Option[Export], NoStream, Effect.Read] =
    regionExports
      .filter(_.email === email)
      .filter(_.createdAt === new Timestamp(createdAt))
      .result
      .map(_.headOption.map(x => exportFromRow(x)))

  def getAll(email: String)
            (implicit ec: ExecutionContext): DBIOAction[Seq[Export], NoStream, Effect.Read] =
    regionExports
      .filter(_.email === email)
      .result
      .map(_.map(exportFromRow))

  def insert(regionExport: Export): DBIOAction[Int, NoStream, Effect.Write] = {
    val (startDate: String, endDate: String, createdAt: Timestamp) = dates(regionExport)
    regionExports += (regionExport.email, regionExport.terminals, startDate, endDate, regionExport.status, createdAt)
  }

  def update(regionExport: Export): FixedSqlAction[Int, NoStream, Effect.Write] = {
    val query = for {
      export <- regionExports if matches(regionExport, export)
    } yield export.status

    query.update(regionExport.status)
  }

  private def matches(regionExport: Export, export: ExportTable): Rep[Boolean] = {
    export.email === regionExport.email && export.terminals === regionExport.terminals && export.createdAt === new Timestamp(regionExport.createdAt.millisSinceEpoch)
  }

  private def exportFromRow(row: (String, String, String, String, String, Timestamp)): Export = {
    val startDate = LocalDate.parse(row._3).getOrElse(throw new Exception(s"Could not parse date ${row._3}"))
    val endDate = LocalDate.parse(row._4).getOrElse(throw new Exception(s"Could not parse date ${row._4}"))
    val createdAt = SDate(row._6.getTime)
    Export(row._1, row._2, startDate, endDate, row._5, createdAt)
  }

  private def dates(regionExport: Export): (String, String, Timestamp) = {
    val createdAt = new Timestamp(regionExport.createdAt.millisSinceEpoch)
    (regionExport.startDate.toISOString, regionExport.endDate.toISOString, createdAt)
  }
}
