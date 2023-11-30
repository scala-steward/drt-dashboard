package uk.gov.homeoffice.drt.db

import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery


trait AppDatabase {
  val profile: slick.jdbc.JdbcProfile

  val db: profile.backend.Database

  lazy val userTable: TableQuery[UserTable] = TableQuery[UserTable]

  lazy val userAccessRequestsTable: TableQuery[UserAccessRequestsTable] = TableQuery[UserAccessRequestsTable]

  lazy val dropInTable: TableQuery[DropInTable] = TableQuery[DropInTable]

  lazy val dropInRegistrationTable: TableQuery[DropInRegistrationTable] = TableQuery[DropInRegistrationTable]

  lazy val regionExportTable: TableQuery[RegionExportTable] = TableQuery[RegionExportTable]

  lazy val exportTable: TableQuery[ExportTable] = TableQuery[ExportTable]
}

object ProdDatabase extends AppDatabase {
  override val profile: JdbcProfile = slick.jdbc.PostgresProfile
  override val db: profile.backend.Database = profile.api.Database.forConfig("postgresDB")
}
