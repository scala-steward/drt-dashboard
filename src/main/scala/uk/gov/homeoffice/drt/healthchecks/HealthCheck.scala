package uk.gov.homeoffice.drt.healthchecks

import uk.gov.homeoffice.drt.time.SDateLike

import scala.concurrent.duration.FiniteDuration
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
  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def passThresholdPercentage: Int

  override val parseResponse: String => HealthCheckResponse[Double] =
    str => {
      val value: Try[Option[Double]] = str match {
        case "null" => Try(None)
        case _ => Try(Option(str.toDouble))
      }
      val maybeIsPass = value.toOption.flatten.map(_ >= passThresholdPercentage)
      log.info(s"HealthCheck $name got response: $str, value: $value, maybeIsPass: $maybeIsPass")

      PercentageHealthCheckResponse(priority, name, value, maybeIsPass)
    }

  override def failure: HealthCheckResponse[Double] =
    PercentageHealthCheckResponse(priority, name, Failure(new Exception("Failed to parse response")), None)
}

case class ApiHealthCheck(hoursBeforeNow: Int, hoursAfterNow: Int, minimumFlights: Int, passThresholdPercentage: Int, now: () => SDateLike) extends PercentageHealthCheck {
  private val start = now().addHours(-hoursBeforeNow)
  private val end = now().addHours(hoursAfterNow)
  override val priority: IncidentPriority = Priority1
  override val name: String = "API received"
  override val description: String = s"""$passThresholdPercentage% of flights landing between ${start.prettyDateTime} and ${end.prettyDateTime} which have API data, when we have a minimum of $minimumFlights flights"""
  override val url: String = s"/health-check/received-api/${start.toISOString}/${end.toISOString}/$minimumFlights"
}

case class ArrivalLandingTimesHealthCheck(windowLength: FiniteDuration, buffer: Int, minimumFlights: Int, passThresholdPercentage: Int, now: () => SDateLike) extends PercentageHealthCheck {
  private val start = now().addMinutes(-windowLength.toMinutes.toInt)
  private val end = now().addMinutes(-buffer)
  override val priority: IncidentPriority = Priority1
  override val name: String = "Landing Times"
  override val description: String = s"$passThresholdPercentage% of flights scheduled to land between ${start.toHoursAndMinutes} and ${end.toHoursAndMinutes} which have an actual landing time, when we have a minimum of $minimumFlights flights"
  override val url: String = s"/health-check/received-landing-times/${start.toISOString}/${end.toISOString}/$minimumFlights"
}

case class ArrivalUpdatesHealthCheck(minutesBeforeNow: Int, minutesAfterNow: Int, updateThreshold: FiniteDuration, minimumFlights: Int, passThresholdPercentage: Int, now: () => SDateLike) extends PercentageHealthCheck {
  private val start = now().addMinutes(-minutesBeforeNow)
  private val end = now().addMinutes(minutesAfterNow)
  override val priority: IncidentPriority = Priority2
  override val name: String = s"Arrival Updates"
  override val description: String = s"$passThresholdPercentage% of flights expected to land between ${start.toHoursAndMinutes} and ${end.toHoursAndMinutes} that have been updated in the past ${updateThreshold.toMinutes} minutes, when we have a minimum of $minimumFlights flights"
  override val url: String = s"/health-check/received-arrival-updates/${start.toISOString}/${end.toISOString}/$minimumFlights/${updateThreshold.toMinutes}"
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
