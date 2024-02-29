package uk.gov.homeoffice.drt.healthchecks

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.Dashboard
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}


object PortHealthCheck {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(port: PortCode,
            makeRequest: HttpRequest => Future[HttpResponse],
            healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable]
           )
           (implicit mat: Materializer, ec: ExecutionContext): Future[Seq[HealthCheckResponse[_ >: Double with Boolean <: AnyVal]]] = {
    Source(healthChecks)
      .mapAsync(healthChecks.size) { check =>
        val uri = Dashboard.drtInternalUriForPortCode(port) + check.url
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
