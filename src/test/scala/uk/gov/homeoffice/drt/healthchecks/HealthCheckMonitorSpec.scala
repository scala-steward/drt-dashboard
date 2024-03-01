package uk.gov.homeoffice.drt.healthchecks

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.homeoffice.drt.healthchecks.alarms.AlarmInactive
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.time.SDate

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class HealthCheckMonitorSpec
  extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  implicit val ec: ExecutionContext = system.dispatcher

  "HealthCheckMonitor" should {
    val now = () => SDate("2024-06-01T12:00")
    val healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] = Seq(
      ApiHealthCheck(hoursBeforeNow = 2, hoursAfterNow = 1, minimumFlights = 4, passThresholdPercentage = 70, now),
      ArrivalLandingTimesHealthCheck(windowLength = 2.hours, buffer = 20, minimumFlights = 3, passThresholdPercentage = 70, now),
      ArrivalUpdatesHealthCheck(minutesBeforeNow = 30, minutesAfterNow = 60, updateThreshold = 30.minutes, minimumFlights = 3, passThresholdPercentage = 25, now, "near"),
      ArrivalUpdatesHealthCheck(minutesBeforeNow = 0, minutesAfterNow = 120, updateThreshold = 6.hours, minimumFlights = 3, passThresholdPercentage = 5, now, "far"),
    )

    "call health check end points for all port and record the responses" in {
      val requestTestProbe = TestProbe("request")
      val recordTestProbe = TestProbe("record")
      val makeRequest = (request: HttpRequest) => {
        requestTestProbe.ref ! request.uri.toString()
        val response = if (request.uri.path.toString().contains("calculated-desk-updates")) "true" else "55.5"
        Future(HttpResponse(entity = response))
      }
      val recordResponse = (portCode: PortCode, response: HealthCheckResponse[_]) => {
        recordTestProbe.ref ! (portCode, response)
        Future.successful(AlarmInactive)
      }
      val ports = List(PortCode("TST"), PortCode("TST2"))
      val healthCheckMonitor = HealthCheckMonitor(makeRequest, recordResponse, ports, healthChecks)
      healthCheckMonitor()

      requestTestProbe.expectMsgAllOf(
        ports.flatMap(port => Seq(
          s"http://${port.iata.toLowerCase}:9000/health-check/received-api/2024-06-01T10:00:00Z/2024-06-01T13:00:00Z/4",
          s"http://${port.iata.toLowerCase}:9000/health-check/received-landing-times/2024-06-01T10:00:00Z/2024-06-01T11:40:00Z/3",
          s"http://${port.iata.toLowerCase}:9000/health-check/received-arrival-updates/2024-05-31T06:00:00Z/2024-06-04T00:00:00Z/3/30",
          s"http://${port.iata.toLowerCase}:9000/health-check/received-arrival-updates/2024-06-01T12:00:00Z/2024-06-06T12:00:00Z/3/360",
        )): _*
      )
      recordTestProbe.expectMsgAllOf(
        ports.flatMap(port => Seq(
          (port, PercentageHealthCheckResponse(Priority1, "API received", Try(Some(55.5)), Option(false))),
          (port, PercentageHealthCheckResponse(Priority1, "Landing Times", Try(Some(55.5)), Option(false))),
          (port, PercentageHealthCheckResponse(Priority2, "Arrival Updates - near", Try(Some(55.5)), Option(true))),
          (port, PercentageHealthCheckResponse(Priority2, "Arrival Updates - far", Try(Some(55.5)), Option(true))),
        )): _*
      )
    }
  }
}
