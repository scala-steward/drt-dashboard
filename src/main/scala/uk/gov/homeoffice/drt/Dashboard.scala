package uk.gov.homeoffice.drt

import org.joda.time.Interval
import org.joda.time.format.PeriodFormatterBuilder

import scala.concurrent.duration.Duration

object Dashboard {

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

  def timeAgoInWords(millis: Long, now: () => Long = () => System.currentTimeMillis()): String = {
    val oneHourMillis = 60 * 60 * 1000L
    val oneDayMillis = oneHourMillis * 24
    val oneWeekMillis = oneDayMillis * 7
    val twoWeeksMillis = oneWeekMillis * 2

    val dateFormat = millis match {
      case t if t < oneHourMillis =>
        new PeriodFormatterBuilder()
          .appendMinutes()
          .appendSuffix(" minute", " minutes")
          .appendSeparator(" ")
          .appendSeconds
          .appendSuffix(" second", " seconds")
          .minimumPrintedDigits(0)
          .printZeroAlways()
          .toFormatter
      case t if t < oneDayMillis =>
        new PeriodFormatterBuilder()
          .appendHours
          .appendSuffix(" hour", " hours")
          .printZeroAlways()
          .toFormatter
      case t if t < twoWeeksMillis =>
        new PeriodFormatterBuilder()
          .appendYears
          .appendSuffix(" year", " years")
          .appendSeparator(" ")
          .appendMonths
          .appendSuffix(" month", " months")
          .appendSeparator(" ")
          .appendWeeks
          .appendSuffix(" week", " weeks")
          .appendSeparator(" ")
          .appendDays
          .appendSuffix(" day", " days")
          .printZeroAlways()
          .toFormatter
      case _ =>
        new PeriodFormatterBuilder()
          .appendYears
          .appendSuffix(" year", " years")
          .appendSeparator(" ")
          .appendMonths
          .appendSuffix(" month", " months")
          .appendSeparator(" ")
          .appendWeeks
          .appendSuffix(" week", " weeks")
          .printZeroAlways()
          .toFormatter
    }

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

  def lessThanThresholdWarningLevel(millis: Long, warnThreshold: Duration, errorThreshold: Duration): AlertLevel = millis match {
    case millis if millis < errorThreshold.toMillis => ErrorStatus
    case millis if millis < warnThreshold.toMillis => WarningStatus
    case _ => OkStatus
  }

  def timeSince(millis: Long) = System.currentTimeMillis() - millis

  def drtUriForPortCode(portCode: String) = s"http://$portCode:9000"

}
