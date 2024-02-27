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

      val retainMaxResponses = 5
      val result = HealthChecksActor.updateState(emptyState, port, successResponse, "test", nowMillis, retainMaxResponses)

      result should ===(Map(port -> Map("test" -> SortedMap(nowMillis -> successResponse))))
    }
  }

  "isHcAlarmActive" should {
    val alarmTriggerConsecutiveFailures = 3
    s"return true if the last $alarmTriggerConsecutiveFailures are all failures" in {
      val port = PortCode("LHR")
      val failureResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Option(false)), Option(false))
      val successResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Some(true)), Option(true))
      val failureState = Map(port -> Map("test" -> SortedMap(1L -> successResponse, 2L -> successResponse, 3L -> failureResponse, 4L -> failureResponse, 5L -> failureResponse)))

      val result = HealthChecksActor.isHcAlarmActive(failureState, port, "test", alarmTriggerConsecutiveFailures)

      result should ===(true)
    }
    s"return false if the last $alarmTriggerConsecutiveFailures are not all failures" in {
      val port = PortCode("LHR")
      val failureResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Option(false)), Option(false))
      val successResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Some(true)), Option(true))
      val failureState = Map(port -> Map("test" -> SortedMap(1L -> failureResponse, 2L -> successResponse, 3L -> successResponse, 4L -> failureResponse, 5L -> failureResponse)))

      val result = HealthChecksActor.isHcAlarmActive(failureState, port, "test", alarmTriggerConsecutiveFailures)

      result should ===(false)
    }
    s"return false if there are fewer than $alarmTriggerConsecutiveFailures failures" in {
      val port = PortCode("LHR")
      val failureResponse = PercentageHealthCheckResponse(Priority1, "test", Success(Option(25.4)), Option(false))
      val successResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Some(true)), Option(true))
      val failureState = Map(port -> Map("test" -> SortedMap(1L -> failureResponse, 2L -> failureResponse, 3L -> successResponse)))

      val result = HealthChecksActor.isHcAlarmActive(failureState, port, "test", alarmTriggerConsecutiveFailures)

      result should ===(false)
    }
  }

  "HealthCheckActor" should {
    val alarmTriggerConsecutiveFailures = 3
    val retainMaxResponses = 5
    var alarmSounded = false
    var alarmSilenced = false
    val soundAlarm: (PortCode, String, IncidentPriority) => Unit = (_, _, _) => alarmSounded = true
    val silenceAlarm: (PortCode, String, IncidentPriority) => Unit = (_, _, _) => alarmSilenced = true
    val now = () => SDate.now().millisSinceEpoch

    val port = PortCode("LHR")
    val failureResponse = BooleanHealthCheckResponse(Priority1, "test", Failure(new Exception("Failed to parse response")), None)
    val nonPassResponse = PercentageHealthCheckResponse(Priority1, "test", Success(Option(0)), Option(false))
    val successResponse = BooleanHealthCheckResponse(Priority1, "test", Success(Some(true)), Option(true))

    "update the state and sound the alarm" in {
      alarmSounded = false
      alarmSilenced = false
      val port = PortCode("LHR")
      val startState = Map(port -> Map("test" -> SortedMap(1L -> successResponse, 2L -> failureResponse, 3L -> failureResponse)))

      val actor = typedSystem.systemActorOf(HealthChecksActor(soundAlarm, silenceAlarm, now, alarmTriggerConsecutiveFailures, retainMaxResponses, startState), "test1")

      val response = actor.ask(replyTo => HealthChecksActor.PortHealthCheckResponse(port, failureResponse, replyTo))
      Await.result(response, 1.second) === AlarmActive

      alarmSounded should ===(true)
      alarmSilenced should ===(false)
    }

    "update the state and silence the alarm" in {
      alarmSounded = false
      alarmSilenced = false
      val startState = Map(port -> Map("test" -> SortedMap(1L -> failureResponse, 2L -> nonPassResponse, 3L -> failureResponse)))

      val actor = typedSystem.systemActorOf(HealthChecksActor(soundAlarm, silenceAlarm, now, alarmTriggerConsecutiveFailures, retainMaxResponses, startState), "test2")

      val response = actor.ask(replyTo => HealthChecksActor.PortHealthCheckResponse(port, successResponse, replyTo))
      Await.result(response, 1.second) === AlarmInactive

      alarmSounded should ===(false)
      alarmSilenced should ===(true)
    }
  }
}
