package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny
import uk.gov.homeoffice.drt.healthchecks.{ApiHealthCheck, ArrivalLandingTimesHealthCheck, ArrivalUpdatesHealthCheck, HealthCheck}
import uk.gov.homeoffice.drt.persistence.MockScheduledHealthCheckPausePersistence
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.HealthCheckRoutes.alarmStatusFormat
import uk.gov.homeoffice.drt.time.SDate

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

class HealthCheckRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  "HealthCheckRoutes" should {
    val now = () => SDate("2024-06-01T12:00")
    val healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] = Seq(
      ApiHealthCheck(hoursBeforeNow = 2, hoursAfterNow = 1, minimumFlights = 4, passThresholdPercentage = 50, now),
      ArrivalLandingTimesHealthCheck(windowLength = 2.hours, buffer = 20, minimumFlights = 3, passThresholdPercentage = 50, now),
      ArrivalUpdatesHealthCheck(minutesBeforeNow = 30, minutesAfterNow = 60, updateThreshold = 30.minutes, minimumFlights = 3, passThresholdPercentage = 25, now),
      ArrivalUpdatesHealthCheck(minutesBeforeNow = 0, minutesAfterNow = 120, updateThreshold = 6.hours, minimumFlights = 3, passThresholdPercentage = 25, now),
    )

    val alarms = Map(PortCode("portCode") -> Map("alarmName" -> true))
    val getAlarmStatuses: () => Future[Map[PortCode, Map[String, Boolean]]] =
      () => Future.successful(alarms)

    "call the corresponding port uri for the port and dates, given no granularity" in {
      Get("/health-checks/alarm-statuses") ~> HealthCheckRoutes(getAlarmStatuses, healthChecks, MockScheduledHealthCheckPausePersistence) ~> check {
        val str = responseAs[String]

        str shouldEqual alarms.toJson.compactPrint
      }
    }
  }
}
