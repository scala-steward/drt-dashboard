package uk.gov.homeoffice.drt.healthchecks

import spray.json._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.QueueJsonResponse
import uk.gov.homeoffice.drt.services.api.v1.serialiser.QueueApiV1JsonFormats
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

trait HealthCheck[A] {
  val priority: IncidentPriority
  val name: String
  def description: String
  def url: String
  val parseResponse: String => HealthCheckResponse[A]
  def httpHeaders: Map[String, String] = Map.empty

  def failure: HealthCheckResponse[A]
}

trait JsonHealthCheck[T] extends HealthCheck[Boolean] {
  def serialise: String => T
  override val parseResponse: String => HealthCheckResponse[Boolean] =
    str => {
      val trySerialise = Try(serialise(str)).map(_ => true)
      val isPass = trySerialise.getOrElse(false)
      BooleanHealthCheckResponse(priority, name, Success(Option(isPass)), Option(isPass))
    }

  override def failure: HealthCheckResponse[Boolean] =
    BooleanHealthCheckResponse(priority, name, Failure(new Exception("Failed to parse response")), None)
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
      log.info(s"HealthCheck '$name' got response: $str, value: $value, maybeIsPass: $maybeIsPass")

      PercentageHealthCheckResponse(priority, name, value, maybeIsPass)
    }

  override def failure: HealthCheckResponse[Double] =
    PercentageHealthCheckResponse(priority, name, Failure(new Exception("Failed to parse response")), None)
}

case class QueuesApiV1HealthCheck(now: () => SDateLike, portCodes: Iterable[PortCode]) extends JsonHealthCheck[QueueJsonResponse] with QueueApiV1JsonFormats {
  override val priority: IncidentPriority = Priority1
  override val name: String = "Queues API v1"
  override def description: String = s"Queues API v1 is reachable and responding with valid json"

  private def todayAt(hour: Int): SDateLike = SDate(now().toUtcDate).addHours(hour)
  private val startHour = 13
  private val endHour = 14
  private val start: SDateLike = todayAt(startHour)
  private val end: SDateLike = todayAt(endHour)
  override def url: String = s"/api/v1/queues?start=${start.toISOString}&end=${end.toISOString}"

  override def httpHeaders: Map[String, String] = Map(
    "X-Forwarded-Email" -> "health-check",
    "X-Forwarded-Groups" -> (portCodes.map(_.iata).toSeq :+ "api-queue-access").mkString(",")
  )

  override def serialise: String => QueueJsonResponse = _.parseJson.convertTo[QueueJsonResponse]
}

case class ApiHealthCheck(hoursBeforeNow: Int, hoursAfterNow: Int, minimumFlights: Int, passThresholdPercentage: Int, now: () => SDateLike) extends PercentageHealthCheck {
  private val start = () => now().addHours(-hoursBeforeNow)
  private val end = () => now().addHours(hoursAfterNow)
  override val priority: IncidentPriority = Priority1
  override val name: String = "API received"
  override def description: String = s"""$passThresholdPercentage% of flights landing between ${start().prettyDateTime} and ${end().prettyDateTime} which have API data, when we have a minimum of $minimumFlights flights"""
  override def url: String = s"/health-check/received-api/${start().toISOString}/${end().toISOString}/$minimumFlights"
}

case class ArrivalLandingTimesHealthCheck(windowLength: FiniteDuration, buffer: Int, minimumFlights: Int, passThresholdPercentage: Int, now: () => SDateLike) extends PercentageHealthCheck {
  private val start = () => now().addMinutes(-windowLength.toMinutes.toInt)
  private val end = () => now().addMinutes(-buffer)
  override val priority: IncidentPriority = Priority1
  override val name: String = "Landing Times"
  override def description: String = s"$passThresholdPercentage% of flights scheduled to land between ${start().toHoursAndMinutes} and ${end().toHoursAndMinutes} which have an actual landing time, when we have a minimum of $minimumFlights flights"
  override def url: String = s"/health-check/received-landing-times/${start().toISOString}/${end().toISOString}/$minimumFlights"
}

case class ArrivalUpdatesHealthCheck(minutesBeforeNow: Int, minutesAfterNow: Int, updateThreshold: FiniteDuration, minimumFlights: Int, passThresholdPercentage: Int, now: () => SDateLike) extends PercentageHealthCheck {
  private val start = () => now().addMinutes(-minutesBeforeNow)
  private val end = () => now().addMinutes(minutesAfterNow)
  override val priority: IncidentPriority = Priority2
  override val name: String = s"Arrival Updates"
  override def description: String = s"$passThresholdPercentage% of flights expected to land between ${start().toHoursAndMinutes} and ${end().toHoursAndMinutes} that have been updated in the past ${updateThreshold.toMinutes} minutes, when we have a minimum of $minimumFlights flights"
  override def url: String = s"/health-check/received-arrival-updates/${start().toISOString}/${end().toISOString}/$minimumFlights/${updateThreshold.toMinutes}"
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
