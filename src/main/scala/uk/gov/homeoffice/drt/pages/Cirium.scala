package uk.gov.homeoffice.drt.pages

import org.joda.time.Interval
import org.joda.time.format.PeriodFormatterBuilder
import scalatags.Text
import scalatags.Text.all._
import uk.gov.homeoffice.cirium.services.health.CiriumAppHealthSummary

import scala.collection.SortedMap
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.language.postfixOps

sealed trait StatusItemLike {

  def value: String

  def alertLevel: AlertLevel
}

sealed trait AlertLevel {
  def className: String
}

case object ErrorStatus extends AlertLevel {
  override def className: String = "table-danger"
}

case object OkStatus extends AlertLevel {
  override def className: String = "table-success"
}

case object InfoStatus extends AlertLevel {
  override def className: String = "table-info"
}

case object WarningStatus extends AlertLevel {
  override def className: String = "table-warning"
}

case class StatusItem(value: String, alertLevel: AlertLevel) extends StatusItemLike

object StatusItem {
  def error(message: String) = StatusItem(message, ErrorStatus)

  def warning(message: String) = StatusItem(message, WarningStatus)

  def ok(message: String) = StatusItem(message, OkStatus)

  def info(message: String) = StatusItem(message, InfoStatus)
}

object Cirium {

  def timeAgoInWords(millis: Long, now: () => Long = () => System.currentTimeMillis()): String = {
    val dateFormat = new PeriodFormatterBuilder()
      .appendYears
      .appendSuffix(" years")
      .appendSeparator(" ")
      .appendWeeks
      .appendSuffix(" weeks")
      .appendSeparator(" ")
      .appendMonths
      .appendSuffix(" months")
      .appendSeparator(" ")
      .appendDays
      .appendSuffix(" days")
      .appendSeparator(" ")
      .appendHours
      .appendSuffix(" hours")
      .appendSeparator(" ")
      .appendMinutes
      .appendSuffix(" minutes")
      .appendSeparator(" ")
      .appendSeconds
      .appendSuffix(" seconds")
      .minimumPrintedDigits(0)
      .printZeroAlways()
      .toFormatter

    val curentTimeMillis = now()
    val intervalMillis = Math.round(millis / 1000) * 1000L
    val interval = new Interval(curentTimeMillis - intervalMillis, curentTimeMillis)
    dateFormat.print(interval.toPeriod)
  }

  def timeWarningLevel(millis: Long, warnThreshold: Duration, errorThreshold: Duration): AlertLevel = millis match {
    case millis if millis > errorThreshold.toMillis => ErrorStatus
    case millis if millis > warnThreshold.toMillis => WarningStatus
    case _ => OkStatus
  }

  def timeSince(millis: Long) = System.currentTimeMillis() - millis

  def apply(data: CiriumAppHealthSummary): Text.TypedTag[String] = {

    val readinessStatus = if (data.feedHealth.isReady) StatusItem.ok("Ready") else StatusItem.warning("Catching Up")

    val lastMessageStatus: StatusItem = data.feedHealth.lastMessage.flatMap(_.messageIssuedAt) match {
      case Some(millis) =>
        val timeAgo = timeSince(millis)
        StatusItem(
          timeAgoInWords(timeAgo),
          timeWarningLevel(timeAgo, 30 seconds, 5 minutes))
      case None => StatusItem.error("Unable to connect to Cirium")
    }

    val lastMessageProcessedStatus = data
      .feedHealth
      .lastMessage.map(msg => (msg.processedMillis, msg.messageIssuedAt)) match {
        case Some((lastProcessed, Some(lastIssued))) =>
          val timeSinceProcessed = timeSince(lastProcessed)
          val timeSinceIssued = timeSince(lastIssued)
          StatusItem(

            timeAgoInWords(timeSinceProcessed),
            timeWarningLevel(timeSinceIssued - timeSinceProcessed, 20 seconds, 40 seconds))
        case Some((lastProcessed, None)) =>
          val timeSinceProcessed = timeSince(lastProcessed)

          StatusItem(
            timeAgoInWords(timeSinceProcessed),
            timeWarningLevel(lastProcessed, 30 seconds, 1 minute))
        case _ => StatusItem.error("Unable to connect to Cirium")
      }

    val upTimeMillis = data.feedHealth.upTime * 1000
    val upTimeStatus = StatusItem(timeAgoInWords(upTimeMillis), InfoStatus)

    val statuses = SortedMap(
      "App Status" -> readinessStatus,
      "Last Message Available" -> lastMessageStatus,
      "Last Message Processed" -> lastMessageProcessedStatus,
      "Uptime" -> upTimeStatus)
    div(

      h1("Cirium Feed Status"),
      div(
        cls := "status-box",
        table(
          cls := "table drt-dashboard__dashboard",
          for ((title, status) <- statuses.toSeq) yield {
            tr(
              cls := status.alertLevel.className,
              td(title),
              td(status.value))
          })))
  }

}
