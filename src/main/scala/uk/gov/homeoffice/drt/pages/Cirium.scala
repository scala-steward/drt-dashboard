package uk.gov.homeoffice.drt.pages

import scalatags.Text
import scalatags.Text.all._
import uk.gov.homeoffice.cirium.services.health.CiriumAppHealthSummary
import uk.gov.homeoffice.drt.Dashboard._

import scala.collection.SortedMap
import scala.concurrent.duration._
import scala.language.postfixOps

object Cirium {

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
          cls := "table cirium-dashboard__dashboard",
          for ((title, status) <- statuses.toSeq) yield {
            tr(
              cls := status.alertLevel.className,
              td(title),
              td(status.value))
          })))
  }

}
