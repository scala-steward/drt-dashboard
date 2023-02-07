package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }
import akka.stream.Materializer
import uk.gov.homeoffice.drt.HttpClient

import scala.concurrent.{ ExecutionContextExecutor, Future }

class MockHttpClient(httpResponse: HttpResponse) extends HttpClient {
  override def send(httpRequest: HttpRequest)(implicit executionContext: ExecutionContextExecutor, mat: Materializer): Future[HttpResponse] = {
    Future(httpResponse)
  }
}
