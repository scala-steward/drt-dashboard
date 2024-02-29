package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import spray.json.enrichAny
import uk.gov.homeoffice.drt.healthchecks.{ApiHealthCheck, ArrivalLandingTimesHealthCheck, ArrivalUpdates120HealthCheck, ArrivalUpdates60HealthCheck, HealthCheck}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.HealthCheckRoutes.alarmStatusFormat

import scala.concurrent.{ExecutionContextExecutor, Future}

class HealthCheckRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  implicit val typedSystem: ActorSystem[Nothing] = ActorSystem.wrap(system)
  implicit val mat: Materializer = Materializer(system)
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  "HealthCheckRoutes" should {
    val healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] = Seq(
      ApiHealthCheck(passThresholdPercentage = 70),
      ArrivalLandingTimesHealthCheck(passThresholdPercentage = 70),
      ArrivalUpdates60HealthCheck(passThresholdPercentage = 25),
      ArrivalUpdates120HealthCheck(passThresholdPercentage = 5),
    )

    val alarms = Map(PortCode("portCode") -> Map("alarmName" -> true))
    val getAlarmStatuses: () => Future[Map[PortCode, Map[String, Boolean]]] =
      () => Future.successful(alarms)

    "call the corresponding port uri for the port and dates, given no granularity" in {
      Get("/health-checks/alarm-statuses") ~> HealthCheckRoutes(getAlarmStatuses, healthChecks) ~> check {
        val str = responseAs[String]

        str shouldEqual alarms.toJson.compactPrint
      }
    }
  }
}
