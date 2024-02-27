package uk.gov.homeoffice.drt.healthchecks

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import uk.gov.homeoffice.drt.healthchecks.alarms.{AlarmActive, AlarmInactive, AlarmState}
import uk.gov.homeoffice.drt.ports.PortCode

import scala.collection.immutable.SortedMap


object HealthChecksActor {
  trait Command

  case class PortHealthCheckResponse(portCode: PortCode, response: HealthCheckResponse[_], replyTo: ActorRef[AlarmState]) extends Command

  def apply(portChecks: Map[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]],
            soundAlarm: (PortCode, String, IncidentPriority) => Unit,
            silenceAlarm: (PortCode, String, IncidentPriority) => Unit,
            now: () => Long,
            alarmTriggerConsecutiveFailures: Int,
           ): Behaviors.Receive[Command] =
    Behaviors.receiveMessage {
      case PortHealthCheckResponse(portCode, response, replyTo) =>
        val checkName = response.name

        val alarmPreviouslyActive = isHcAlarmActive(portChecks, portCode, checkName, alarmTriggerConsecutiveFailures)
        val newPortChecks = updateState(portChecks, portCode, response, checkName, now())
        val alarmNowActive = isHcAlarmActive(newPortChecks, portCode, checkName, alarmTriggerConsecutiveFailures)

        if (!alarmNowActive && alarmPreviouslyActive)
          silenceAlarm(portCode, checkName, response.priority)
        if (alarmNowActive && !alarmPreviouslyActive)
          soundAlarm(portCode, checkName, response.priority)

        replyTo ! (if (alarmNowActive) AlarmActive else AlarmInactive)

        HealthChecksActor(newPortChecks, soundAlarm, silenceAlarm, now, alarmTriggerConsecutiveFailures)
    }

  def updateState(state: Map[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]], portCode: PortCode, response: HealthCheckResponse[_], checkName: String, now: Long): Map[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]] = {
    val portResponses = state.getOrElse(portCode, Map.empty)
    val hcResponses = getHcResponses(portResponses, checkName)
    val newHcResponses = if (hcResponses.size >= 3)
      hcResponses.drop(1) + (now -> response)
    else
      hcResponses + (now -> response)
    val newPortResponses = portResponses.updated(checkName, newHcResponses)
    state.updated(portCode, newPortResponses)
  }

  private def getHcResponses(portResponses: Map[String, SortedMap[Long, HealthCheckResponse[_]]],
                             checkName: String): SortedMap[Long, HealthCheckResponse[_]] =
    portResponses.getOrElse(checkName, SortedMap.empty[Long, HealthCheckResponse[_]])

  def isHcAlarmActive(portChecks: Map[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]],
                      portCode: PortCode,
                      checkName: String,
                      alarmTriggerConsecutiveFailures: Int,
                     ): Boolean = {
    val portResponses = portChecks.getOrElse(portCode, Map.empty)
    val hcResponses = getHcResponses(portResponses, checkName)

    if (hcResponses.size >= alarmTriggerConsecutiveFailures)
      hcResponses
        .takeRight(alarmTriggerConsecutiveFailures).values
        .forall(r => r.value.isFailure || r.maybeIsPass.contains(false))
    else
      false
  }
}
