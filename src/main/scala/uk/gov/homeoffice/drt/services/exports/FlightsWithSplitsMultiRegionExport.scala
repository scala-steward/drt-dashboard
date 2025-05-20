package uk.gov.homeoffice.drt.services.exports

import uk.gov.homeoffice.drt.arrivals.{ApiFlightWithSplits, ArrivalExportHeadings}
import uk.gov.homeoffice.drt.models.{PassengerInfo, VoyageManifest}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode, PortRegion}
import uk.gov.homeoffice.drt.services.exports.FlightExports.{actualAPISplitsForFlightInHeadingOrder, ageRangesFromSummary, nationalitiesFromSummary}
import uk.gov.homeoffice.drt.time.LocalDate


trait FlightsWithSplitsMultiRegionExport extends FlightsWithSplitsExport {
  override val headings: String = "Region,Port," +
    ArrivalExportHeadings.arrivalWithSplitsAndRawApiHeadings

  val portCode: PortCode
  private lazy val region: String = PortRegion.fromPort(portCode).name
  private lazy val regionAndPort: List[String] = List(region, portCode.iata)

  override val prepend: Option[String] = None

  override def rowValues(fws: ApiFlightWithSplits, maybeManifest: Option[VoyageManifest]): Seq[String] = {
    val maybePaxSummary = maybeManifest.flatMap(PassengerInfo.manifestToFlightManifestSummary)

    (regionAndPort ::: flightWithSplitsToCsvRow(fws) :::
      actualAPISplitsForFlightInHeadingOrder(fws, ArrivalExportHeadings.actualApiHeadings.split(",")).toList).map(s => s"$s") :::
      List(s""""${nationalitiesFromSummary(maybePaxSummary)}"""", s""""${ageRangesFromSummary(maybePaxSummary)}"""")
  }
}

case class FlightsWithSplitsMultiRegionExportImpl(start: LocalDate,
                                                  end: LocalDate,
                                                  portCode: PortCode,
                                                  terminals: Seq[Terminal],
                                                  paxFeedSourceOrder: List[FeedSource],
                                                 ) extends FlightsWithSplitsMultiRegionExport {
  override val flightsFilter: (ApiFlightWithSplits, Seq[Terminal]) => Boolean = standardFilter
}
