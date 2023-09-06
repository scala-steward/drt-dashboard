package uk.gov.homeoffice.drt.exports

sealed trait ExportType {
  val routePrefix: String
}

case object Arrivals extends ExportType {
  override val routePrefix: String = "arrivals"
}

case object PassengersDaily extends ExportType {
  override val routePrefix: String = "passengers"
}
