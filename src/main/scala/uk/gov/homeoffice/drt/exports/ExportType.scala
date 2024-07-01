package uk.gov.homeoffice.drt.exports

import uk.gov.homeoffice.drt.arrivals.ArrivalExportHeadings.regionalExportHeadings
import uk.gov.homeoffice.drt.ports.Queues
import uk.gov.homeoffice.drt.ports.Queues.displayName

sealed trait ExportType {
  val routePrefix: String
  val headerRow: String
}

sealed trait DailyExportType extends ExportType
sealed trait PortExportType extends ExportType

case object Arrivals extends ExportType {
  override val routePrefix: String = "arrivals"
  override val headerRow: String = regionalExportHeadings
}

case object PortPassengers extends ExportType with PortExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Region,Port,Capacity,PCP passengers,$queueHeadings"
}

case object TerminalPassengers extends ExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Region,Port,Terminal,Capacity,PCP passengers,$queueHeadings"
}

case object PortPassengersDaily extends DailyExportType with PortExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Date,Region,Port,Capacity,PCP passengers,$queueHeadings"
}

case object TerminalPassengersDaily extends DailyExportType {
  private val queueHeadings: String = Queues.queueOrder.map(q => displayName(q)).mkString(",")

  override val routePrefix: String = "passengers"
  override val headerRow: String = s"Date,Region,Port,Terminal,Capacity,PCP passengers,$queueHeadings"
}
