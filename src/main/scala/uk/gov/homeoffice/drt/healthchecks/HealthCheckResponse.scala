package uk.gov.homeoffice.drt.healthchecks

import scala.util.Try

trait HealthCheckResponse[A] {
  val priority: IncidentPriority
  val name: String
  val value: Try[Option[A]]
  val maybeIsPass: Option[Boolean]
}

case class PercentageHealthCheckResponse(priority: IncidentPriority,
                                         name: String,
                                         value: Try[Option[Double]],
                                         maybeIsPass: Option[Boolean]) extends HealthCheckResponse[Double]
case class BooleanHealthCheckResponse(priority: IncidentPriority,
                                      name: String,
                                      value: Try[Option[Boolean]],
                                      maybeIsPass: Option[Boolean]) extends HealthCheckResponse[Boolean]
