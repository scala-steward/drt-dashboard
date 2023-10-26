package uk.gov.homeoffice.drt.healthchecks

import akka.Done
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import uk.gov.homeoffice.drt.healthchecks.alarms.AlarmState
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}

object HealthCheckMonitor {
  def apply(makeRequest: HttpRequest => Future[HttpResponse],
            recordResponse: (PortCode, HealthCheckResponse[_]) => Future[AlarmState],
            ports: Iterable[PortCode]
           )
           (implicit mat: Materializer, ec: ExecutionContext): () => Future[Done] =
    () => Source(ports.toList)
      .mapAsync(1) { port =>
        println("checking port " + port)
        PortHealthCheck(port, makeRequest).map(_.map(r => (port, r)))
      }
      .mapConcat(identity)
      .mapAsync(1) {
        case (port, response) => recordResponse(port, response)
      }
      .runWith(Sink.ignore)
      .recover {
        case t: Throwable =>
          println("HealthCheckMonitor failed: " + t.getMessage)
          Done
      }
}
