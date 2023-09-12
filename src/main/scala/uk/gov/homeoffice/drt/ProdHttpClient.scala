package uk.gov.homeoffice.drt

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.Materializer
import org.slf4j.{Logger, LoggerFactory}
import spray.json._
import uk.gov.homeoffice.drt.DashboardClient._
import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.auth.Roles.Role
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.FlightData

import scala.concurrent.{ExecutionContextExecutor, Future}

trait HttpClient extends JsonSupport {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def send(httpRequest: HttpRequest)(implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[HttpResponse]

  def createPortArrivalImportRequest(uri: String, portCode: PortCode): HttpRequest = {
    val headersWithRoles = rolesToRoleHeader(List(
      Option(Roles.ArrivalsAndSplitsView), Option(Roles.ApiView), Roles.parse(portCode.iata)
    ).flatten)
    HttpRequest(method = HttpMethods.GET, uri = uri, headers = headersWithRoles)
  }

  def createDrtNeboRequest(data: List[FlightData], uri: String, portAccessRole: Option[Role]): HttpRequest = {
    log.info(s"Sending json to drt for $uri with ${data.size} flight details")
    HttpRequest(
      method = HttpMethods.POST,
      uri = uri,
      headers = rolesToRoleHeader(List(Option(Roles.NeboUpload), portAccessRole).flatten),
      entity = HttpEntity(ContentTypes.`application/json`, data.toJson.toString()))
  }
}

object ProdHttpClient extends HttpClient {
  def send(httpRequest: HttpRequest)(implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[HttpResponse] = {
    Http()(mat.system).singleRequest(httpRequest)
  }
}
