package uk.gov.homeoffice.drt.json

import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.models.Export
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

class ExportJsonFormatsTest extends AnyWordSpec {
  "RegionExport" should {
    "serialise and deserialise without loss" in {
      val export = Export("email", "stn-t1__lhr-t2_lhr-t3", LocalDate(2020, 1, 1), LocalDate(2020, 1, 2), "status", SDate(2020, 1, 1))
      val json = ExportJsonFormats.exportJsonFormat.write(export)
      val deserialised = ExportJsonFormats.exportJsonFormat.read(json)
      assert(deserialised == export)
    }
  }
}
