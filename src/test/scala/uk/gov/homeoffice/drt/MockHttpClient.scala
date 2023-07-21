package uk.gov.homeoffice.drt

import akka.http.scaladsl.model._
import akka.stream.Materializer

import scala.concurrent.{ExecutionContextExecutor, Future}

case class MockHttpClient(content: () => String) extends HttpClient {
  override def send(httpRequest: HttpRequest)(implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[HttpResponse] = {
    Future(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, content())))
  }
}
