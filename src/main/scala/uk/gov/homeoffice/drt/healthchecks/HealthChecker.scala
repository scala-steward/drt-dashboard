package uk.gov.homeoffice.drt.healthchecks

import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.Dashboard
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}


object HealthChecker {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(maybePort: Option[PortCode],
            makeRequest: HttpRequest => Future[HttpResponse],
            healthChecks: Seq[HealthCheck[_]]
           )
           (implicit mat: Materializer, ec: ExecutionContext): Future[Seq[HealthCheckResponse[_]]] = {
    Source(healthChecks)
      .mapAsync(healthChecks.size) { check =>
        val uri = maybePort match {
          case Some(port) => Dashboard.drtInternalUriForPortCode(port) + check.url
          case None => Dashboard.drtInternalUri + check.url
        }
        val request = HttpRequest(uri = uri)
        val startTime = System.currentTimeMillis()
        makeRequest(request)
          .flatMap { response =>
            val status = response.status
            if (status.isSuccess()) {
              response.entity.dataBytes
                .map(_.utf8String)
                .runReduce(_ + _)
                .map(check.parseResponse)
            } else {
              response.entity.discardBytes()
              Future.successful(check.failure)
            }
          }
          .recover {
            case t: Throwable =>
              val timeTaken = System.currentTimeMillis() - startTime
              log.warn(s"${check.name} failed after ${timeTaken / 1000}s: ${t.getMessage}")
              check.failure
          }
      }
      .runWith(Sink.seq)
      .recover {
        case t: Throwable =>
          log.warn(s"All checks failed: ${t.getMessage}")
          Seq.empty
      }
  }
}
