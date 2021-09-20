package uk.gov.homeoffice.drt.services

import com.github.tototoshi.csv.CSVReader

import scala.io.Source

case class IsoCountries(headings: List[String], rows: List[Map[String, String]]) {
  def startingWith(prefix: String): List[Map[String, String]] = rows.collect {
    case row if row.values.exists(_.toLowerCase.startsWith(prefix)) => row
  }
}

object IsoCountries {
  def apply(source: Source): IsoCountries = {
    val csv = CSVReader.open(source)
    val (headings, rows) = csv.allWithOrderedHeaders()
    IsoCountries(headings, rows)
  }
}
