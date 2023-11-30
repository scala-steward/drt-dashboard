package uk.gov.homeoffice.drt.db

import com.typesafe.config.ConfigFactory

object Db {
  val slickProfile = if (ConfigFactory.load().getString("env") != "test")
    slick.jdbc.PostgresProfile
  else
    slick.jdbc.H2Profile
}
