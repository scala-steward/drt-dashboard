package uk.gov.homeoffice.drt.alerts

import akka.actor.ClassicActorSystemProvider
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding.Post
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpHeader, HttpResponse }
import org.joda.time.{ DateTime, DateTimeZone }
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }
import uk.gov.homeoffice.drt.auth.Roles.Role

import scala.concurrent.Future

case class MultiPortAlert(
  title: String,
  message: String,
  alertClass: String,
  expires: String,
  alertPorts: Map[String, Boolean]) {
  def alertForPorts(allPorts: List[String]): Map[String, Alert] = allPorts
    .collect {
      case pc if !alertPorts.getOrElse(pc, false) =>
        pc -> Alert(title, message, alertClass, Dates.localDateStringToMillis(expires))
    }.toMap
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

object AlertClient {

  def postWithRoles(uri: String, json: String, roles: Iterable[Role])(implicit system: ClassicActorSystemProvider): Future[HttpResponse] = {
    val roleHeader: Option[HttpHeader] = HttpHeader.parse("X-Auth-Roles", roles.mkString(",")) match {
      case Ok(header, _) => Option(header)
      case _ => None
    }

    Http().singleRequest(Post(uri, HttpEntity(ContentTypes.`text/plain(UTF-8)`, json)).withHeaders(roleHeader.toList))
  }
}
