package uk.gov.homeoffice.drt.healthchecks

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import uk.gov.homeoffice.drt.Dashboard
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}


trait PercentageHealthCheck extends HealthCheck[Double] {
  override val parseResponse: String => HealthCheckResponse[Double] =
    str => {
      val value: Try[Option[Double]] = str match {
        case "null" => Try(None)
        case _ => Try(Option(str.toDouble))
      }

      PercentageHealthCheckResponse(priority, name, value)
    }

  override def failure: HealthCheckResponse[Double] = PercentageHealthCheckResponse(priority, name, Failure(new Exception("Failed to parse response")))
}

trait BooleanHealthCheck extends HealthCheck[Boolean] {
  override val parseResponse: String => HealthCheckResponse[Boolean] =
    str => {
      val value: Try[Option[Boolean]] = str match {
        case "null" => Try(None)
        case _ => Try(Option(str.toBoolean))
      }

      BooleanHealthCheckResponse(priority, name, value)
    }

  override def failure: HealthCheckResponse[Boolean] = BooleanHealthCheckResponse(priority, name, Failure(new Exception("Failed to parse response")))
}

case object ApiHealthCheck extends PercentageHealthCheck {
  override val priority: FailurePriority = Priority1
  override val name: String = "API"
  override val url: String = "/health-check/received-api/60/10"
}

case object ArrivalLandingTimesHealthCheck extends PercentageHealthCheck {
  override val priority: FailurePriority = Priority1
  override val name: String = "Arrival Landing Times"
  override val url: String = "/health-check/received-landing-times/300/1"
}

case object ArrivalUpdates60HealthCheck extends PercentageHealthCheck {
  override val priority: FailurePriority = Priority2
  override val name: String = "Arrival Updates 60"
  override val url: String = "/health-check/received-arrival-updates/60/3"
}

case object ArrivalUpdates120HealthCheck extends PercentageHealthCheck {
  override val priority: FailurePriority = Priority2
  override val name: String = "Arrival Updates 120"
  override val url: String = "/health-check/received-arrival-updates/120/1"
}

case object DeskUpdatesHealthCheck extends BooleanHealthCheck {
  override val priority: FailurePriority = Priority1
  override val name: String = "Desk Updates"
  override val url: String = "/health-check/calculated-desk-updates"
}

object PortHealthCheck {
  private val checks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] = Seq(
    ApiHealthCheck,
    ArrivalLandingTimesHealthCheck,
    ArrivalUpdates60HealthCheck,
    ArrivalUpdates120HealthCheck,
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
