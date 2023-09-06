package uk.gov.homeoffice.drt.exports

import uk.gov.homeoffice.drt.arrivals.ArrivalExportHeadings.regionalExportHeadings
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.ports.Queues.displayName

sealed trait ExportType {
  val routePrefix: String
  val headerRow: String
}

case object Arrivals extends ExportType {
  override val routePrefix: String = "arrivals"
  override val headerRow: String = regionalExportHeadings
}

case object PassengersDaily extends ExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Date,Region,Port,Terminal,Total passengers,PCP passengers,Transit passengers,$queueHeadings"
}
