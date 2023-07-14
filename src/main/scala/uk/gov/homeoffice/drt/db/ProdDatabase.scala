package uk.gov.homeoffice.drt.db

import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery


trait AppDatabase {
  val profile: slick.jdbc.JdbcProfile

  lazy val userTable: TableQuery[UserTable] = TableQuery[UserTable]

  lazy val userAccessRequestsTable: TableQuery[UserAccessRequestsTable] = TableQuery[UserAccessRequestsTable]

  lazy val regionExportTable: TableQuery[RegionExportTable] = TableQuery[RegionExportTable]
}

object ProdDatabase extends AppDatabase {
  override val profile: JdbcProfile = slick.jdbc.PostgresProfile
  val db: profile.backend.Database = profile.api.Database.forConfig("postgresDB")
}
