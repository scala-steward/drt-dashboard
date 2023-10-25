package uk.gov.homeoffice.drt.healthchecks

import scala.util.{Failure, Success, Try}

trait HealthCheck[A] {
  val priority: IncidentPriority
  val name: String
  val url: String
  val parseResponse: String => HealthCheckResponse[A]

  def failure: HealthCheckResponse[A]
}

trait PercentageHealthCheck extends HealthCheck[Double] {
  def pass: Double => Boolean

  override val parseResponse: String => HealthCheckResponse[Double] =
    str => {
      val value: Try[Option[Double]] = str match {
        case "null" => Try(None)
        case _ => Try(Option(str.toDouble))
      }
      val isPass = value.toOption.flatten.map(pass)

      PercentageHealthCheckResponse(priority, name, value, isPass)
    }

  override def failure: HealthCheckResponse[Double] =
    PercentageHealthCheckResponse(priority, name, Failure(new Exception("Failed to parse response")), None)
}

trait BooleanHealthCheck extends HealthCheck[Boolean] {
  override val parseResponse: String => HealthCheckResponse[Boolean] =
    str => {
      val value: Try[Option[Boolean]] = str match {
        case "null" => Try(None)
        case _ => Try(Option(str.toBoolean))
      }
      val isPass = value.toOption.flatten

      BooleanHealthCheckResponse(priority, name, value, isPass)
    }

  override def failure: HealthCheckResponse[Boolean] =
    BooleanHealthCheckResponse(priority, name, Failure(new Exception("Failed to parse response")), None)
}

case class ApiHealthCheck(pass: Double => Boolean) extends PercentageHealthCheck {
  override val priority: IncidentPriority = Priority1
  override val name: String = "API"
  override val url: String = "/health-check/received-api/60/10"
}

case class ArrivalLandingTimesHealthCheck(pass: Double => Boolean) extends PercentageHealthCheck {
  override val priority: IncidentPriority = Priority1
  override val name: String = "Arrival Landing Times"
  override val url: String = "/health-check/received-landing-times/300/1"
}

case class ArrivalUpdates60HealthCheck(pass: Double => Boolean) extends PercentageHealthCheck {
  override val priority: IncidentPriority = Priority2
  override val name: String = "Arrival Updates 60"
  override val url: String = "/health-check/received-arrival-updates/60/3"
}

case class ArrivalUpdates120HealthCheck(pass: Double => Boolean) extends PercentageHealthCheck {
  override val priority: IncidentPriority = Priority2
  override val name: String = "Arrival Updates 120"
  override val url: String = "/health-check/received-arrival-updates/120/1"
}

case object DeskUpdatesHealthCheck extends BooleanHealthCheck {
  override val priority: IncidentPriority = Priority1
  override val name: String = "Desk Updates"
  override val url: String = "/health-check/calculated-desk-updates"
}

trait IncidentPriority

case object Priority1 extends IncidentPriority

case object Priority2 extends IncidentPriority
