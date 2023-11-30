package uk.gov.homeoffice.drt

import akka.http.scaladsl.model._
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

case class MockHttpClient(content: () => String) extends HttpClient {
  override def send(httpRequest: HttpRequest)
                   (implicit executionContext: ExecutionContext, mat: Materializer): Future[HttpResponse] =
    Future(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, content())))
}
