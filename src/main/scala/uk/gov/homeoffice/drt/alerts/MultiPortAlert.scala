package uk.gov.homeoffice.drt.alerts

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpResponse
import org.joda.time.{DateTime, DateTimeZone}
import org.slf4j.{Logger, LoggerFactory}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.PortAlerts
import uk.gov.homeoffice.drt.{Dashboard, DashboardClient}

import scala.collection.immutable
import scala.concurrent.Future

case class MultiPortAlert(
  title: String,
  message: String,
  alertClass: String,
  expires: String,
  alertPorts: List[String]) {

  def alertForPorts(allPorts: Iterable[String]): Map[String, Alert] = allPorts
    .collect {
      case pc if alertPorts.map(_.toLowerCase).contains(pc.toLowerCase) =>
        pc -> Alert(title, message, alertClass, Dates.localDateStringToMillis(expires))
    }.toMap
}

object Dates {
  def localDateStringToMillis(dateString: String): Long = new DateTime(
    dateString,
    DateTimeZone.forID("Europe/London")).getMillis
}

case class Alert(title: String, message: String, alertClass: String, expires: Long)

trait MultiPortAlertJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val MultiPortAlertFormatParser: RootJsonFormat[MultiPortAlert] = jsonFormat5(MultiPortAlert)
  implicit val AlertFormatParser: RootJsonFormat[Alert] = jsonFormat4(Alert)
  implicit val portAlertsJsonFormat: RootJsonFormat[PortAlerts] = jsonFormat2(PortAlerts)
}

object MultiPortAlertClient extends MultiPortAlertJsonSupport {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def saveAlertsForPorts(portCodes: Iterable[String], multiPortAlert: MultiPortAlert, user: User)(implicit system: ActorSystem[Nothing]): immutable.Iterable[Future[HttpResponse]] =
    multiPortAlert.alertForPorts(portCodes).map {
      case (portCode, alert) =>
        log.info(s"Sending new alert to ${Dashboard.drtUriForPortCode(portCode)}/alerts")
        DashboardClient.postWithRoles(
          s"${Dashboard.drtInternalUriForPortCode(PortCode(portCode))}/alerts",
          alert.toJson.toString(),
          user.roles)
    }
}
