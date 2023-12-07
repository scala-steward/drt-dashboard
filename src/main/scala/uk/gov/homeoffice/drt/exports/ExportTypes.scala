package uk.gov.homeoffice.drt.exports

object ExportTypes {

  def parse(exportType: String): ExportType = exportType match {
    case "arrivals" => Arrivals
    case "passengers-port" => PortPassengers
    case "passengers-terminal" => TerminalPassengers
    case "passengers-port-daily" => PortPassengersDaily
    case "passengers-terminal-daily" => TerminalPassengersDaily
    case _ => throw new Exception(s"Unknown export type $exportType")
  }
}
