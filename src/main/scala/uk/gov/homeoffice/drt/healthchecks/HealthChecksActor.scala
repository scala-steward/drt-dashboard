package uk.gov.homeoffice.drt.healthchecks

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import uk.gov.homeoffice.drt.healthchecks.alarms.{AlarmActive, AlarmInactive, AlarmState}
import uk.gov.homeoffice.drt.ports.PortCode

import scala.collection.immutable.SortedMap


object HealthChecksActor {
  trait Command

  case class PortHealthCheckResponse(portCode: PortCode,
                                     response: HealthCheckResponse[_],
                                     replyTo: ActorRef[AlarmState],
                                    ) extends Command

  case class GetAlarmStatuses(replyTo: ActorRef[Map[PortCode, Map[String, Boolean]]]) extends Command

  def apply(soundAlarm: (PortCode, String, IncidentPriority) => Unit,
            silenceAlarm: (PortCode, String, IncidentPriority) => Unit,
            now: () => Long,
            alarmTriggerConsecutiveFailures: Int,
            retainMaxResponses: Int,
            portChecks: Map[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]]): Behaviors.Receive[Command] = {

    def behaviour(checks: Map[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]]): Behaviors.Receive[Command] =
      Behaviors.receiveMessage {
        case GetAlarmStatuses(replyTo) =>
          replyTo ! checks.map {
            case (portCode, portResponses) =>
              portCode -> portResponses.map {
                case (checkName, _) => checkName -> isHcAlarmActive(checks, portCode, checkName, alarmTriggerConsecutiveFailures)
              }
          }
          Behaviors.same
        case PortHealthCheckResponse(portCode, response, replyTo) =>
          val checkName = response.name

          val alarmPreviouslyActive = isHcAlarmActive(checks, portCode, checkName, alarmTriggerConsecutiveFailures)
          val newPortChecks = updateState(checks, portCode, response, checkName, now(), retainMaxResponses)
          val alarmNowActive = isHcAlarmActive(newPortChecks, portCode, checkName, alarmTriggerConsecutiveFailures)

          if (!alarmNowActive && alarmPreviouslyActive)
            silenceAlarm(portCode, checkName, response.priority)
          if (alarmNowActive && !alarmPreviouslyActive)
            soundAlarm(portCode, checkName, response.priority)

          replyTo ! (if (alarmNowActive) AlarmActive else AlarmInactive)

          behaviour(newPortChecks)
      }

    behaviour(portChecks)
  }

  def updateState(state: Map[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]],
                  portCode: PortCode,
                  response: HealthCheckResponse[_],
                  checkName: String,
                  now: Long,
                  retainMaxResponses: Int,
                 ): Map[PortCode, Map[String, SortedMap[Long, HealthCheckResponse[_]]]] = {
    val portResponses = state.getOrElse(portCode, Map.empty)
    val hcResponses = getHcResponses(portResponses, checkName)
    val newHcResponses = if (hcResponses.size >= retainMaxResponses)
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
