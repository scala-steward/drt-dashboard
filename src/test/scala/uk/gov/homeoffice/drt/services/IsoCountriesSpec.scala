package uk.gov.homeoffice.drt.services

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.io.Source

class IsoCountriesSpec extends AnyWordSpec with Matchers {
  val csvContent: String =
    """ISO name,DRT name,ISO code
      |Afghanistan,Afghanistan,AFG
      |""".stripMargin

  val mockSource: Source = Source.fromString(csvContent)
  val realSource: Source = Source.fromResource("iso-countries.csv")

  val mockCountries: IsoCountries = IsoCountries(mockSource)
  val realCountries: IsoCountries = IsoCountries(realSource)

  "A Countries object" should {
    "be able to read csv headings in the correct format" in {
      val expectedHeadings = List("ISO name", "DRT name", "ISO code")

      mockCountries.headings should ===(expectedHeadings)
    }

    "be able to read csv rows in the correct format" in {
      val expectedRows = List(Map("ISO name" -> "Afghanistan", "DRT name" -> "Afghanistan", "ISO code" -> "AFG"))

      mockCountries.rows should ===(expectedRows)
    }

    "be able to give all countries starting with a set of characters" in {
      val startingChars = "bel"
      val matches = realCountries.startingWith(startingChars)

      matches should ===(List(
        Map("ISO name" -> "Belarus", "DRT name" -> "Belarus", "ISO code" -> "BLR"),
        Map("ISO name" -> "Belgium", "DRT name" -> "Belgium", "ISO code" -> "BEL"),
        Map("ISO name" -> "Belize", "DRT name" -> "Belize", "ISO code" -> "BLZ")))
    }
  }
}
