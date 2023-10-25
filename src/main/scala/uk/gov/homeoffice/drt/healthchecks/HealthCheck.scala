package uk.gov.homeoffice.drt.healthchecks

trait HealthCheck[A] {
  val priority: FailurePriority
  val name: String
  val url: String
  val parseResponse: String => HealthCheckResponse[A]

  def failure: HealthCheckResponse[A]
}

trait FailurePriority

case object Priority1 extends FailurePriority

case object Priority2 extends FailurePriority
