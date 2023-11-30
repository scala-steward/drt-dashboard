package uk.gov.homeoffice.drt.healthchecks

import org.joda.time.DateTime
import uk.gov.homeoffice.drt.ports.PortCode

case class ScheduledPause(startsAt: DateTime, endsAt: DateTime, ports: Iterable[PortCode], createdAt: DateTime)
