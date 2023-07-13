package uk.gov.homeoffice.drt.db

import slick.jdbc.H2Profile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

object AppTestDatabase {
  lazy val db: Database = Database.forConfig("h2-db")

  val userTable: TableQuery[UserTable] = TableQuery[UserTable]

  def createDbStructure: Future[Unit] = db.run(DBIO.seq(userTable.schema.dropIfExists, userTable.schema.create))
}

