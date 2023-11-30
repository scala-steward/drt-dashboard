package uk.gov.homeoffice.drt.persistence

import uk.gov.homeoffice.drt.db.{AppDatabase, ExportQueries}
import uk.gov.homeoffice.drt.models.Export

import scala.concurrent.{ExecutionContext, Future}

trait ExportPersistence {
  def insert(export: Export): Future[Int]

  def update(export: Export): Future[Int]

  def get(email: String, createdAt: Long): Future[Option[Export]]

  def getAll(email: String): Future[Seq[Export]]
}

case class ExportPersistenceImpl(database: AppDatabase)
                                (implicit ec: ExecutionContext) extends ExportPersistence {
  override def insert(export: Export): Future[Int] = database.db.run(ExportQueries.insert(export))

  override def update(export: Export): Future[Int] = database.db.run(ExportQueries.update(export))

  override def get(email: String, createdAt: Long): Future[Option[Export]] = database.db.run(ExportQueries.get(email, createdAt))

  override def getAll(email: String): Future[Seq[Export]] = database.db.run(ExportQueries.getAll(email))
}
