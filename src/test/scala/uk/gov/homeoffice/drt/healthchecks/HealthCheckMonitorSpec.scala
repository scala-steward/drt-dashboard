package uk.gov.homeoffice.drt.healthchecks

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.homeoffice.drt.healthchecks.alarms.AlarmInactive
import uk.gov.homeoffice.drt.ports.PortCode

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
    val healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] = Seq(
      ApiHealthCheck(passThresholdPercentage = 70),
      ArrivalLandingTimesHealthCheck(passThresholdPercentage = 70),
      ArrivalUpdates60HealthCheck(passThresholdPercentage = 25),
      ArrivalUpdates120HealthCheck(passThresholdPercentage = 5),
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
          s"http://${port.iata.toLowerCase}:9000/health-check/received-api/60/10",
          s"http://${port.iata.toLowerCase}:9000/health-check/received-landing-times/300/1",
          s"http://${port.iata.toLowerCase}:9000/health-check/received-arrival-updates/60/3/30",
          s"http://${port.iata.toLowerCase}:9000/health-check/received-arrival-updates/120/2/360",
        )): _*
      )
      recordTestProbe.expectMsgAllOf(
        ports.flatMap(port => Seq(
          (port, PercentageHealthCheckResponse(Priority1, "API received", Try(Some(55.5)), Option(false))),
          (port, PercentageHealthCheckResponse(Priority1, "Landing Times", Try(Some(55.5)), Option(false))),
          (port, PercentageHealthCheckResponse(Priority2, "Arrival Updates - 1hr", Try(Some(55.5)), Option(true))),
          (port, PercentageHealthCheckResponse(Priority2, "Arrival Updates - 2hrs", Try(Some(55.5)), Option(true))),
        )): _*
      )
    }
  }
}
