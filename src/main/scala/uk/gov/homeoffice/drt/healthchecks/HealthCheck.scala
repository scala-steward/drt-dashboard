package uk.gov.homeoffice.drt.healthchecks

trait HealthCheck[A] {
  val name: String
  val url: String
  val parseResponse: String => HealthCheckResponse[A]

  def failure: HealthCheckResponse[A]
}
