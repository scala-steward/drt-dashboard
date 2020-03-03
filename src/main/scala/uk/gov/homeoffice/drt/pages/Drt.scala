package uk.gov.homeoffice.drt.pages

import scalatags.Text
import scalatags.Text.all._
import uk.gov.homeoffice.drt.Dashboard._
import uk.gov.homeoffice.drt.services.drt.DashboardPortStatus

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

object Drt {

  case class DrtFeedDisplay(sourceName: String,
                            displayName: String,
                            successWarningThreshold: FiniteDuration,
                            successErrorThreshold: FiniteDuration,
                            updatedWarningThreshold: FiniteDuration,
                            updatedErrorThreshold: FiniteDuration,
                            failureLessThanWarningThreshold: FiniteDuration,
                            failureLessThanErrorThreshold: FiniteDuration,
                           )

  val feedOrder = Seq(
    DrtFeedDisplay("ApiFeedSource",
      "API",
      successWarningThreshold = 2 minutes,
      successErrorThreshold = 10 minutes,
      updatedWarningThreshold = 10 minutes,
      updatedErrorThreshold = 2 hours,
      failureLessThanWarningThreshold = 1 day,
      failureLessThanErrorThreshold = 3 hours
    ),
    DrtFeedDisplay("AclFeedSource",
      "ACL",
      successWarningThreshold = 15 minutes,
      successErrorThreshold = 30 minutes,
      updatedWarningThreshold = 6 hours,
      updatedErrorThreshold = 1 day,
      failureLessThanWarningThreshold = 1 day,
      failureLessThanErrorThreshold = 3 hours
    ),
    DrtFeedDisplay("LiveBaseFeedSource",
      "Cirium",
      successWarningThreshold = 1 minute,
      successErrorThreshold = 5 minutes,
      updatedWarningThreshold = 10 minutes,
      updatedErrorThreshold = 30 minutes,
      failureLessThanWarningThreshold = 1 day,
      failureLessThanErrorThreshold = 3 hours
    ),
    DrtFeedDisplay("LiveFeedSource",
      "Port Live",
      successWarningThreshold = 2 minutes,
      successErrorThreshold = 10 minutes,
      updatedWarningThreshold = 16 minutes,
      updatedErrorThreshold = 30 minutes,
      failureLessThanWarningThreshold = 1 day,
      failureLessThanErrorThreshold = 3 hours
    ),
    DrtFeedDisplay("ForecastFeedSource",
      "Port Forecast",
      successWarningThreshold = 5 minutes,
      successErrorThreshold = 20 minutes,
      updatedWarningThreshold = 7 days,
      updatedErrorThreshold = 14 days,
      failureLessThanWarningThreshold = 1 day,
      failureLessThanErrorThreshold = 3 hours
    )
  )

  def apply(portStatuses: List[DashboardPortStatus]): Text.TypedTag[String] = {

    div(

      h1("DRT Port Status"),
      div(
        cls := "status-box",
        table(
          cls := "table drt-dashboard__dashboard",
          tr(
            td("Port"),
            feedOrder.map(f => td(colspan := 3, f.displayName))),
          tr(
            cls := "drt-dashboard__dashboard__last-status",
            td(),
            feedOrder.map(_ => Seq(td("Last Success"), td("Last Failure"), td("Last Updated")))),
          for (status <- portStatuses) yield {
            tr(
              td(status.portCode),
              feedOrder.map(f => {
                status.feedStatuses.byFeed.get(f.sourceName) match {
                  case Some(s) => Seq(
                    td(
                      cls := maybeTimestampToWarningLevelClass(
                        s.lastSuccessAt,
                        f.successWarningThreshold,
                        f.successErrorThreshold,
                        timeWarningLevel
                      ),
                      maybeTimestampToWords(s.lastSuccessAt)
                    ),
                    td(
                      cls := maybeTimestampToWarningLevelClass(
                        s.lastUpdatesAt,
                        f.updatedWarningThreshold,
                        f.updatedErrorThreshold,
                        timeWarningLevel
                      ),
                      maybeTimestampToWords(s.lastUpdatesAt)
                    ),
                    td(
                      cls := maybeTimestampToWarningLevelClass(
                        s.lastFailureAt,
                        f.failureLessThanWarningThreshold,
                        f.successErrorThreshold,
                        lessThanThresholdWarningLevel
                      ),
                      maybeTimestampToWords(s.lastFailureAt)
                    )
                  )
                  case _ =>
                    List.fill(3)(td(cls := InfoStatus.className))
                }
              }))
          })))

  }


  def maybeTimestampToWords(maybeLong: Option[Long]) = maybeLong
    .map(l => timeAgoInWords(timeSince(l))).getOrElse("")

  def maybeTimestampToWarningLevelClass(maybeLastEvent: Option[Long], warningThreshold: FiniteDuration, errorThreshold: FiniteDuration, errorThresholdFunction: (Long, Duration, Duration) => AlertLevel) = maybeLastEvent
    .map(l => errorThresholdFunction(timeSince(l), warningThreshold, errorThreshold).className).getOrElse(InfoStatus.className)

  def uriForPortCode(portCode: String) = {
    "http://localhost:9000/feed-statuses"
  }
}

