package uk.gov.homeoffice.drt.db

import slick.jdbc.PostgresProfile
import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._

object AppDatabase {

  lazy val db: PostgresProfile.backend.Database = Database.forConfig("postgresDB")

  val userTable = TableQuery[UserTable]

  val userAccessRequestsTable: TableQuery[UserAccessRequestsTable] = TableQuery[UserAccessRequestsTable]

}
