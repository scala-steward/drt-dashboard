package uk.gov.homeoffice.drt.healthchecks

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import uk.gov.homeoffice.drt.Dashboard
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}


object PortHealthCheck {
  private val checks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] = Seq(
    ApiHealthCheck(_ >= 70),
    ArrivalLandingTimesHealthCheck(_ >= 70),
    ArrivalUpdates60HealthCheck(_ >= 25),
    ArrivalUpdates120HealthCheck(_ >= 5),
    DeskUpdatesHealthCheck,
  )

  def apply(port: PortCode, makeRequest: HttpRequest => Future[HttpResponse])
           (implicit mat: Materializer, ec: ExecutionContext): Future[Seq[HealthCheckResponse[_ >: Double with Boolean <: AnyVal]]] = {
    Source(checks)
      .mapAsync(1) { check =>
        val uri = Dashboard.drtInternalUriForPortCode(port) + check.url
        val request = HttpRequest(uri = uri)
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
      }
      .runWith(Sink.seq)
  }
}
