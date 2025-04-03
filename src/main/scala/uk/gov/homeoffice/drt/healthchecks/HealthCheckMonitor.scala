package uk.gov.homeoffice.drt.healthchecks

import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.healthchecks.alarms.AlarmState
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}

object HealthCheckMonitor {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(makeRequest: HttpRequest => Future[HttpResponse],
            recordResponse: (PortCode, HealthCheckResponse[_]) => Future[AlarmState],
            ports: Iterable[PortCode],
            healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable],
           )
           (implicit mat: Materializer, ec: ExecutionContext): () => Future[Done] =
    () => Source(ports.toList)
      .mapAsync(1) { port =>
        log.info("Checking port " + port)
        PortHealthCheck(port, makeRequest, healthChecks).map(_.map { r =>
          log.info(s"HealthCheckMonitor got response for $port: ${r.name} -> ${r.maybeIsPass}")
          (port, r)
        })
      }
      .mapConcat(identity)
      .mapAsync(1) {
        case (port, response) => recordResponse(port, response)
      }
      .runWith(Sink.ignore)
      .recover {
        case t: Throwable =>
          log.error("HealthCheckMonitor failed: " + t.getMessage)
          Done
      }
}
