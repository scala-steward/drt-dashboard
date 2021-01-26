package uk.gov.homeoffice.drt.alerts

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.{ DateTime, DateTimeZone }
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }

case class MultiPortAlert(
  title: String,
  message: String,
  alertClass: String,
  expires: String,
  alertPorts: Map[String, Boolean]) {

  def alertForPorts(allPorts: List[String]): Map[String, Alert] = {
    println(allPorts)
    println(alertPorts)
    allPorts
      .collect {
        case pc if alertPorts.getOrElse(pc.toUpperCase, false) =>
          pc -> Alert(title, message, alertClass, Dates.localDateStringToMillis(expires))
      }.toMap
  }
}

object Dates {
  def localDateStringToMillis(dateString: String): Long = new DateTime(
    dateString,
    DateTimeZone.forID("Europe/London")).getMillis
}

case class Alert(title: String, message: String, alertClass: String, expires: Long)

object MultiPortAlertJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val MultiPortAlertFormatParser: RootJsonFormat[MultiPortAlert] = jsonFormat5(MultiPortAlert)
  implicit val AlertFormatParser: RootJsonFormat[Alert] = jsonFormat4(Alert)
}
