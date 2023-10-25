package uk.gov.homeoffice.drt.healthchecks

import scala.util.Try

trait HealthCheckResponse[A] {
  val priority: FailurePriority
  val name: String
  val value: Try[Option[A]]
}

case class PercentageHealthCheckResponse(priority: FailurePriority, name: String, value: Try[Option[Double]]) extends HealthCheckResponse[Double]
case class BooleanHealthCheckResponse(priority: FailurePriority, name: String, value: Try[Option[Boolean]]) extends HealthCheckResponse[Boolean]
