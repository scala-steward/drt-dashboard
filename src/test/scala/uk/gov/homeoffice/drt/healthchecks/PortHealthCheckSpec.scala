package uk.gov.homeoffice.drt.healthchecks

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Success

object MockHttp {
  def withResponse(percent: String, boolean: String)
                  (implicit ec: ExecutionContext): HttpRequest => Future[HttpResponse] = request => {
    val response = if (request.uri.path.toString().contains("calculated-desk-updates")) boolean else percent
    Future(HttpResponse(entity = response))
  }

  def withFailureResponse()
                         (implicit ec: ExecutionContext): HttpRequest => Future[HttpResponse] = _ => {
    Future(HttpResponse(status = BadRequest))
  }
}

class PortHealthCheckSpec
  extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = system.dispatcher

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "PortHealthCheck" should {
    val healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] = Seq(
      ApiHealthCheck(passThresholdPercentage = 70),
      ArrivalLandingTimesHealthCheck(passThresholdPercentage = 70),
      ArrivalUpdates60HealthCheck(passThresholdPercentage = 25),
      ArrivalUpdates120HealthCheck(passThresholdPercentage = 5),
    )

    "parse successful responses" in {
      val responses = PortHealthCheck(PortCode("TST"), MockHttp.withResponse("50.5", "true"), healthChecks)

      Await.result(responses, 1.second) should ===(Seq(
        PercentageHealthCheckResponse(Priority1, "API received", Success(Some(50.5)), Option(false)),
        PercentageHealthCheckResponse(Priority1, "Landing Times", Success(Some(50.5)), Option(false)),
        PercentageHealthCheckResponse(Priority2, "Arrival Updates - 1hr", Success(Some(50.5)), Option(true)),
        PercentageHealthCheckResponse(Priority2, "Arrival Updates - 2hrs", Success(Some(50.5)), Option(true)),
      ))
    }
    "parse null responses" in {
      val responses = PortHealthCheck(PortCode("TST"), MockHttp.withResponse("null", "null"), healthChecks)

      Await.result(responses, 1.second) should ===(Seq(
        PercentageHealthCheckResponse(Priority1, "API received", Success(None), None),
        PercentageHealthCheckResponse(Priority1, "Landing Times", Success(None), None),
        PercentageHealthCheckResponse(Priority2, "Arrival Updates - 1hr", Success(None), None),
        PercentageHealthCheckResponse(Priority2, "Arrival Updates - 2hrs", Success(None), None),
      ))
    }
    "handle failed responses" in {
      val responses = PortHealthCheck(PortCode("TST"), MockHttp.withFailureResponse(), healthChecks)

      Await.result(responses, 1.second).map(_.value.isFailure) should ===(Seq(true, true, true, true))
    }
  }
}
