package uk.gov.homeoffice.drt.healthchecks

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.{ActorSystem, typed}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import uk.gov.homeoffice.drt.healthchecks.alarms.{AlarmActive, AlarmInactive}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.time.SDate

import scala.collection.immutable.SortedMap
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class HealthChecksActorSpec
  extends TestKit(ActorSystem("MySpec"))
    with ImplicitSender
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll {

  implicit val ec: ExecutionContext = system.dispatcher
  implicit val timeout: Timeout = new Timeout(1.second)
  val typedSystem: typed.ActorSystem[Nothing] = akka.actor.typed.ActorSystem.wrap(system)

  implicit val scheduler: typed.Scheduler = typedSystem.scheduler

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "updateState" should {
    "update the state" in {
      val emptyState = Map.empty[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]]
      val port = PortCode("LHR")
      val successResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Some(true)), Option(true))

      val nowMillis = SDate.now().millisSinceEpoch

      val result = HealthChecksActor.updateState(emptyState, port, successResponse, "test", nowMillis)

      result === Map(port -> Map("test" -> SortedMap(nowMillis -> successResponse)))
    }
  }

  "isHcAlarmActive" should {
    val alarmTriggerConsecutiveFailures = 3
    s"return true if there are $alarmTriggerConsecutiveFailures failures" in {
      val port = PortCode("LHR")
      val failureResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Option(false)), Option(false))
      val failureState = Map(port -> Map("test" -> SortedMap(1L -> failureResponse, 2L -> failureResponse, 3L -> failureResponse)))

      val result = HealthChecksActor.isHcAlarmActive(failureState, port, "test", alarmTriggerConsecutiveFailures)

      result === true
    }
    s"return false if there are less than $alarmTriggerConsecutiveFailures failures" in {
      val port = PortCode("LHR")
      val failureResponse = PercentageHealthCheckResponse(Priority1, "test", Success(Option(25.4)), Option(false))
      val successResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Some(true)), Option(true))
      val failureState = Map(port -> Map("test" -> SortedMap(1L -> failureResponse, 2L -> failureResponse, 3L -> successResponse)))

      val result = HealthChecksActor.isHcAlarmActive(failureState, port, "test", alarmTriggerConsecutiveFailures)

      result === false
    }
  }

  "HealthCheckActor" should {
    "update the state and sound the alarm" in {
      var alarmSounded = false
      var alarmSilenced = false
      val soundAlarm: (PortCode, String, IncidentPriority) => Unit = (_, _, _) => alarmSounded = true
      val silenceAlarm: (PortCode, String, IncidentPriority) => Unit = (_, _, _) => alarmSilenced = true
      val now = () => SDate.now().millisSinceEpoch
      val alarmTriggerConsecutiveFailures = 3

      val port = PortCode("LHR")
      val failureResponse = BooleanHealthCheckResponse(Priority1, "test", Failure(new Exception("Failed to parse response")), None)
      val successResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Some(true)), Option(true))
      val startState = Map(port -> Map("test" -> SortedMap(1L -> successResponse, 2L -> failureResponse, 3L -> failureResponse)))

      val actor = typedSystem.systemActorOf(HealthChecksActor(startState, soundAlarm, silenceAlarm, now, alarmTriggerConsecutiveFailures), "test1")

      val response = actor.ask(replyTo => HealthChecksActor.PortHealthCheckResponse(port, failureResponse, replyTo))
      Await.result(response, 1.second) === AlarmActive

      alarmSounded === true
      alarmSilenced === false
    }

    "update the state and silence the alarm" in {
      var alarmSounded = false
      var alarmSilenced = false
      val soundAlarm: (PortCode, String, IncidentPriority) => Unit = (_, _, _) => alarmSounded = true
      val silenceAlarm: (PortCode, String, IncidentPriority) => Unit = (_, _, _) => alarmSilenced = true
      val now = () => SDate.now().millisSinceEpoch
      val alarmTriggerConsecutiveFailures = 3

      val port = PortCode("LHR")
      val failureResponse = BooleanHealthCheckResponse(Priority1, "test", Failure(new Exception("Failed to parse response")), None)
      val nonPassResponse = PercentageHealthCheckResponse(Priority1, "test", Success(Option(0)), Option(false))
      val successResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Some(true)), Option(true))
      val startState = Map(port -> Map("test" -> SortedMap(1L -> failureResponse, 2L -> nonPassResponse, 3L -> failureResponse)))

      val actor = typedSystem.systemActorOf(HealthChecksActor(startState, soundAlarm, silenceAlarm, now, alarmTriggerConsecutiveFailures), "test2")

      val response = actor.ask(replyTo => HealthChecksActor.PortHealthCheckResponse(port, successResponse, replyTo))
      Await.result(response, 1.second) === AlarmInactive

      alarmSounded === false
      alarmSilenced === true
    }
  }
}
