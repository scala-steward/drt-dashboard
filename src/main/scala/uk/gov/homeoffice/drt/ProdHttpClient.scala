package uk.gov.homeoffice.drt

import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.Accept
import akka.stream.Materializer
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.drt.DashboardClient._
import uk.gov.homeoffice.drt.auth.Roles
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def send(httpRequest: HttpRequest): Future[HttpResponse]

  def httpRequestForPortCsv(uri: String, portCode: PortCode): HttpRequest = {
    val roleHeaders = rolesToRoleHeader(List(
      Option(Roles.ArrivalsAndSplitsView), Option(Roles.ApiView), Roles.parse(portCode.iata)
    ).flatten)
    HttpRequest(
      method = HttpMethods.GET,
      uri = uri,
      headers = roleHeaders :+ Accept(MediaTypes.`text/csv`)
    )
  }
}

case class ProdHttpClient(sendHttpRequest: HttpRequest => Future[HttpResponse])(implicit val ec: ExecutionContext, val mat: Materializer) extends HttpClient {
  def send(httpRequest: HttpRequest): Future[HttpResponse] =
    sendHttpRequest(httpRequest)
      .recover {
        case e: Throwable =>
          log.error(s"Failed to connect to ${httpRequest.uri}", e)
          throw e
      }
}
