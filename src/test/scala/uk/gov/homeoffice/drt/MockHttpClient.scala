package uk.gov.homeoffice.drt

import akka.http.scaladsl.model._
import akka.stream.scaladsl.Source
import akka.testkit.TestProbe
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}

case class MockHttpClient(content: () => String, maybeProbe: Option[TestProbe] = None)
                         (implicit ec: ExecutionContext)extends HttpClient {
  override def send(httpRequest: HttpRequest): Future[HttpResponse] = {
    maybeProbe.foreach(_.ref ! httpRequest)
    val entity = content() match {
      case "" => HttpEntity(ContentTypes.`text/csv(UTF-8)`, Source.empty[ByteString])
      case str => HttpEntity(ContentTypes.`text/csv(UTF-8)`, str)
    }
    Future(HttpResponse(StatusCodes.OK, entity = entity))
  }
}
