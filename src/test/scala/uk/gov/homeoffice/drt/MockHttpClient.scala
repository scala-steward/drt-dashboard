package uk.gov.homeoffice.drt

import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.testkit.TestProbe

import scala.concurrent.{ExecutionContext, Future}

case class MockHttpClient(content: () => String, maybeProbe: Option[TestProbe] = None) extends HttpClient {
  override def send(httpRequest: HttpRequest)
                   (implicit executionContext: ExecutionContext, mat: Materializer): Future[HttpResponse] = {
    maybeProbe.foreach {
      _.ref ! httpRequest
    }
    Future(HttpResponse(StatusCodes.OK, entity = HttpEntity(ContentTypes.`text/csv(UTF-8)`, content())))
  }
}
