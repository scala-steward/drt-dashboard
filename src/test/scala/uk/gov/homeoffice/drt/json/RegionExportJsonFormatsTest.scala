package uk.gov.homeoffice.drt.json

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.models.RegionExport
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

class RegionExportJsonFormatsTest extends AnyWordSpec {
  "RegionExport" should {
    "serialise and deserialise without loss" in {
      val regionExport = RegionExport("email", "region", LocalDate(2020, 1, 1), LocalDate(2020, 1, 2), "status", SDate(2020, 1, 1))
      val json = RegionExportJsonFormats.regionExportJsonFormat.write(regionExport)
      val deserialised = RegionExportJsonFormats.regionExportJsonFormat.read(json)
      assert(deserialised == regionExport)
    }
  }
}
