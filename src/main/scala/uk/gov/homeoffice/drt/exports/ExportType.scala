package uk.gov.homeoffice.drt.exports

import uk.gov.homeoffice.drt.arrivals.ArrivalExportHeadings.regionalExportHeadings
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.ports.Queues.displayName

sealed trait ExportType {
  val routePrefix: String
  val headerRow: String
}

sealed trait DailyExportType extends ExportType
sealed trait TerminalExportType extends ExportType

case object Arrivals extends ExportType {
  override val routePrefix: String = "arrivals"
  override val headerRow: String = regionalExportHeadings
}

case object PortPassengers extends ExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Region,Port,Total passengers,PCP passengers,Transit passengers,$queueHeadings"
}

case object TerminalPassengers extends ExportType with TerminalExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Region,Port,Terminal,Total passengers,PCP passengers,Transit passengers,$queueHeadings"
}

case object PortPassengersDaily extends DailyExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Date,Region,Port,Total passengers,PCP passengers,Transit passengers,$queueHeadings"
}

case object TerminalPassengersDaily extends DailyExportType with TerminalExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Date,Region,Port,Terminal,Total passengers,PCP passengers,Transit passengers,$queueHeadings"
}
