package uk.gov.homeoffice.drt.healthchecks

import org.apache.pekko.Done
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.healthchecks.alarms.AlarmState
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}

object HealthChecksRunner {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(makeRequest: HttpRequest => Future[HttpResponse],
            recordResponse: (PortCode, HealthCheckResponse[_]) => Future[AlarmState],
            healthChecks: Seq[HealthCheck[_]],
           )
           (implicit mat: Materializer, ec: ExecutionContext): Option[Iterable[PortCode]] => Future[Done] =
    maybePorts => {
      val checks = maybePorts match {
        case Some(ports) if ports.nonEmpty =>
          log.info("Checking ports")
          Source(ports.toList)
            .mapAsync(1) { port =>
              log.info("Checking port " + port)
              HealthChecker(Option(port), makeRequest, healthChecks).map(_.map { r =>
                log.info(s"HealthCheckMonitor got response for $port: ${r.name} -> ${r.maybeIsPass}")
                (port, r)
              })
            }
        case _ =>
          log.info("Checking dashboard")
          Source.future {
            HealthChecker(None, makeRequest, healthChecks).map(_.map { r =>
              log.info(s"HealthCheckMonitor got response for dashboard: ${r.name} -> ${r.maybeIsPass}")
              (PortCode("Dashboard"), r)
            })
          }
      }

      checks
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
}
