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
    "parse successful responses" in {
      val responses = PortHealthCheck(PortCode("TST"), MockHttp.withResponse("50.5", "true"))

      Await.result(responses, 1.second) should ===(Seq(
        PercentageHealthCheckResponse(Priority1, "API", Success(Some(50.5)), Option(false)),
        PercentageHealthCheckResponse(Priority1, "Arrival Landing Times", Success(Some(50.5)), Option(false)),
        PercentageHealthCheckResponse(Priority2, "Arrival Updates 60", Success(Some(50.5)), Option(true)),
        PercentageHealthCheckResponse(Priority2, "Arrival Updates 120", Success(Some(50.5)), Option(true)),
        BooleanHealthCheckResponse(Priority1, "Desk Updates", Success(Some(true)), Option(true)),
      ))
    }
    "parse null responses" in {
      val responses = PortHealthCheck(PortCode("TST"), MockHttp.withResponse("null", "null"))

      Await.result(responses, 1.second) should ===(Seq(
        PercentageHealthCheckResponse(Priority1, "API", Success(None), None),
        PercentageHealthCheckResponse(Priority1, "Arrival Landing Times", Success(None), None),
        PercentageHealthCheckResponse(Priority2, "Arrival Updates 60", Success(None), None),
        PercentageHealthCheckResponse(Priority2, "Arrival Updates 120", Success(None), None),
        BooleanHealthCheckResponse(Priority1, "Desk Updates", Success(None), None),
      ))
    }
    "handle failed responses" in {
      val responses = PortHealthCheck(PortCode("TST"), MockHttp.withFailureResponse())

      Await.result(responses, 1.second).map(_.value.isFailure) should ===(Seq(true, true, true, true, true))
    }
  }
}
