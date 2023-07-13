package uk.gov.homeoffice.drt.db

import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

class AppTestDatabase {
  lazy val dc: DatabaseConfig[JdbcProfile] = DatabaseConfig.forConfig[JdbcProfile]("h2-db")
  lazy val db = dc.db
  def userTestTable(tableName: String): TableQuery[UserTable] =
    TableQuery[UserTable](tag => new UserTable(tag, tableName))

  def createDbStructure(tableName: String): Unit = {
    val sql =
      s"""CREATE TABLE IF NOT EXISTS $tableName
      (
          id text NOT NULL,
          username text NOT NULL,
          email text NOT NULL,
          latest_login timestamp NOT NULL,
          inactive_email_sent timestamp,
          revoked_access timestamp,
          PRIMARY KEY (id)
      );"""
    val stmt = db.createSession().createStatement()
    try {
      stmt.execute(sql)
    } finally {
      stmt.close()
    }
  }

}

