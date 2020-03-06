package uk.gov.homeoffice.drt.services.drt

case class DashboardPortStatus(portCode: String, feedStatuses: PortFeedStatuses)

object DashboardPortStatus {
  def apply(portCode: String, feedSourceStatuses: List[FeedSourceStatus]): DashboardPortStatus = {
    DashboardPortStatus(portCode, PortFeedStatuses(feedSourceStatuses.map(fss => {
      PortFeedStatus(
        fss.feedSource,
        stringListToLongOption(fss.feedStatuses.lastSuccessAt),
        stringListToLongOption(fss.feedStatuses.lastFailureAt),
        stringListToLongOption(fss.feedStatuses.lastUpdatesAt))
    })))
  }

  def stringListToLongOption(strings: List[String]): Option[Long] = strings.headOption.map(_.toLong)
}

case class PortFeedStatuses(statuses: List[PortFeedStatus]) {
  def byFeed: Map[String, PortFeedStatus] = statuses.map(s => s.feedSource -> s).toMap
}

case class PortFeedStatus(
  feedSource: String,
  lastSuccessAt: Option[Long],
  lastFailureAt: Option[Long],
  lastUpdatesAt: Option[Long])

