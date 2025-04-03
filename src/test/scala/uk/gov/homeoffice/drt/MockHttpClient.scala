package uk.gov.homeoffice.drt

import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.testkit.TestProbe
import org.apache.pekko.util.ByteString

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
