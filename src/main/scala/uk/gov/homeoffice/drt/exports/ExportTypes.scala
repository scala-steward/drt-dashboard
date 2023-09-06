package uk.gov.homeoffice.drt.exports

object ExportTypes {

  def parse(exportType: String): ExportType = exportType match {
    case "arrivals" => Arrivals
    case "passengers" => PassengersDaily
    case _ => throw new Exception(s"Unknown export type $exportType")
  }
}
