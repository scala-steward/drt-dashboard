package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import uk.gov.homeoffice.drt.healthchecks.ScheduledPause
import uk.gov.homeoffice.drt.ports.PortCode

object ScheduledPauseJsonFormats extends DefaultJsonProtocol {
  implicit val portCodeJsonFormat: RootJsonFormat[PortCode] = jsonFormat(
    PortCode.apply,
    "iata"
  )

  import JodaDateTimeJsonFormat.jodaDateTimeJsonFormat

  implicit val scheduledPauseJsonFormat: RootJsonFormat[ScheduledPause] = jsonFormat4(ScheduledPause)
}
