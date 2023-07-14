package uk.gov.homeoffice.drt.db

import slick.jdbc.JdbcProfile

object TestDatabase extends AppDatabase {
  override val profile: JdbcProfile = slick.jdbc.H2Profile
  val db: profile.backend.Database = profile.api.Database.forConfig("h2-db")
}
