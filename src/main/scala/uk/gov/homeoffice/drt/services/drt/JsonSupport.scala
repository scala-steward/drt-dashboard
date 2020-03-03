package uk.gov.homeoffice.drt.services.drt

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

case class FeedSourceStatus(feedSource: String, feedStatuses: FeedStatuses)

case class FeedStatuses(
  lastSuccessAt: List[String],
  lastFailureAt: List[String],
  lastUpdatesAt: List[String])

object JsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val feedFeedStatusesFormat: RootJsonFormat[FeedStatuses] = jsonFormat3(FeedStatuses)
  implicit val feedSourceStatusFormat: RootJsonFormat[FeedSourceStatus] = jsonFormat2(FeedSourceStatus)

}
