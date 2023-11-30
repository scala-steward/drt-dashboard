package uk.gov.homeoffice.drt.healthchecks.alarms

trait AlarmState

case object AlarmActive extends AlarmState

case object AlarmInactive extends AlarmState
