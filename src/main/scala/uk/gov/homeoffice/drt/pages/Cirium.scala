package uk.gov.homeoffice.drt.pages

import org.joda.time.Interval
import org.joda.time.format.PeriodFormatterBuilder
import scalatags.Text
import scalatags.Text.all._
import uk.gov.homeoffice.cirium.services.health.CiriumAppHealthSummary

object Cirium {

  def timeAgo(millis: Long, now: () => Long = () => System.currentTimeMillis()): String = {
    val dateFormat = new PeriodFormatterBuilder()
      .appendDays()
      .appendSuffix(" days")
      .appendSeparator(" ")
      .appendHours()
      .appendSuffix(" hours")
      .appendSeparator(" ")
      .appendMinutes()
      .appendSuffix(" minutes")
      .appendSeparator(" ")
      .appendSeconds()
      .appendSuffix(" seconds")
      .toFormatter();

    val curentTimeMillis = now()
    val interval = new Interval(curentTimeMillis - millis, curentTimeMillis)
    dateFormat.print(interval.toPeriod)
  }

  def timeSince(millis: Long) = System.currentTimeMillis() - millis

  def apply(data: CiriumAppHealthSummary): Text.TypedTag[String] = div(
    h1("Cirium Feed Status"),
    div(cls := "status-box", h2("Connection"),
      table(
        tr(td("App Status"), td(if (data.feedHealth.isReady) "Ready" else "Catching Up")),
        tr(
          td("Last Message Issued"),
          td {
            data.feedHealth.lastMessage.flatMap(_.messageIssuedAt.map(issued => {
              timeAgo(timeSince(issued))
            })).getOrElse("None")
          }
        ),
        tr(
          td("Last Message Processed"),
          td {
            data.feedHealth.lastMessage.map(msg => {
              timeAgo(timeSince(msg.processedMillis))
            }).getOrElse("None")
          }
        ),
        tr(
          td("Up For"),
          td(timeAgo(data.feedHealth.upTime * 1000L))
        )
      )
    )
  )

}


