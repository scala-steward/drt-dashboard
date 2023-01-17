package uk.gov.homeoffice.drt.db

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

class AppTestDatabase {
  lazy val dc: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("h2_dc")
  lazy val db = dc.db
  lazy val userTestTable: TableQuery[UserTable] =
    TableQuery[UserTable](tag => new UserTable(tag, "user_test"))
}

