package uk.gov.homeoffice.drt.healthchecks

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.{Failure, Try}

trait HealthCheck[A] {
  val priority: IncidentPriority
  val name: String
  val description: String
  val url: String
  val parseResponse: String => HealthCheckResponse[A]

  def failure: HealthCheckResponse[A]
}

trait PercentageHealthCheck extends HealthCheck[Double] {
  def passThresholdPercentage: Int

  override val parseResponse: String => HealthCheckResponse[Double] =
    str => {
      val value: Try[Option[Double]] = str match {
        case "null" => Try(None)
        case _ => Try(Option(str.toDouble))
      }
      val maybeIsPass = value.toOption.flatten.map(_ >= passThresholdPercentage)

      PercentageHealthCheckResponse(priority, name, value, maybeIsPass)
    }

  override def failure: HealthCheckResponse[Double] =
    PercentageHealthCheckResponse(priority, name, Failure(new Exception("Failed to parse response")), None)
}

case class ApiHealthCheck(passThresholdPercentage: Int) extends PercentageHealthCheck {
  private val windowLength: FiniteDuration = 60.minutes
  private val minimumFlights: Int = 10
  override val priority: IncidentPriority = Priority1
  override val name: String = "API received"
  override val description: String = s"""$passThresholdPercentage% of flights landed in the past ${windowLength.toMinutes} minutes which have API data, when we have a minimum of $minimumFlights flights"""
  override val url: String = s"/health-check/received-api/${windowLength.toMinutes}/$minimumFlights"
}

case class ArrivalLandingTimesHealthCheck(passThresholdPercentage: Int) extends PercentageHealthCheck {
  private val windowLength: FiniteDuration = 300.minutes
  private val minimumFlights: Int = 1
  override val priority: IncidentPriority = Priority1
  override val name: String = "Landing Times"
  override val description: String = s"$passThresholdPercentage% of flights scheduled to land between ${windowLength.toMinutes} minutes ago and 15 minutes ago which have an actual landing time, when we have a minimum of $minimumFlights flights"
  override val url: String = s"/health-check/received-landing-times/${windowLength.toMinutes}/$minimumFlights"
}

case class ArrivalUpdates60HealthCheck(passThresholdPercentage: Int) extends PercentageHealthCheck {
  private val windowLength: FiniteDuration = 60.minutes
  private val minimumFlights: Int = 3
  private val updateThresholdMinutes: FiniteDuration = 30.minutes
  override val priority: IncidentPriority = Priority2
  override val name: String = "Arrival Updates - 1hr"
  override val description: String = s"$passThresholdPercentage% of flights expected to land in the next ${windowLength.toMinutes} minutes that have been updated in the past ${updateThresholdMinutes.toMinutes} minutes, when we have a minimum of $minimumFlights flights"
  override val url: String = s"/health-check/received-arrival-updates/${windowLength.toMinutes}/$minimumFlights/${updateThresholdMinutes.toMinutes}}"
}

case class ArrivalUpdates120HealthCheck(passThresholdPercentage: Int) extends PercentageHealthCheck {
  private val windowLength: FiniteDuration = 120.minutes
  private val minimumFlights: Int = 2
  private val updateThresholdMinutes: FiniteDuration = 6.hours
  override val priority: IncidentPriority = Priority2
  override val name: String = "Arrival Updates - 2hrs"
  override val description: String = s"$passThresholdPercentage% of flights expected to land in the next ${windowLength.toMinutes} minutes that have been updated in the past ${updateThresholdMinutes.toHours} hours, when we have a minimum of $minimumFlights flights"
  override val url: String = s"/health-check/received-arrival-updates/${windowLength.toMinutes}/$minimumFlights/${updateThresholdMinutes.toMinutes}"
}

trait IncidentPriority {
  val name: String
}

case object Priority1 extends IncidentPriority {
  override val name: String = "P1"
}

case object Priority2 extends IncidentPriority {
  override val name: String = "P2"
}
