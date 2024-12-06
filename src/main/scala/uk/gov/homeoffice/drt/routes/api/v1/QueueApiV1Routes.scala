package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory
import spray.json._
import uk.gov.homeoffice.drt.auth.Roles.ApiQueueAccess
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.model.CrunchMinute
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.services.api.v1.serialiser.QueueApiV1JsonFormats
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.Future
import scala.util.{Failure, Success}


object QueueApiV1Routes extends DefaultJsonProtocol with QueueApiV1JsonFormats {
  private val log = LoggerFactory.getLogger(getClass)

  case class QueueJson(queue: Queue, incomingPax: Int, maxWaitMinutes: Int)

  object QueueJson {
    def apply(cm: CrunchMinute): QueueJson = QueueJson(cm.queue, cm.paxLoad.toInt, cm.waitTime)
  }

  case class SlotJson(slotStartTime: SDateLike, portCode: PortCode, terminal: Terminal, queues: Iterable[QueueJson])

  case class QueueJsonResponse(periodStart: SDateLike, periodEnd: SDateLike, slotSizeMinutes: Int, slots: Seq[SlotJson])

  def apply(enabledPorts: Iterable[PortCode],
            dateRangeJsonForPortsAndSlotSize: (Seq[PortCode], Int) => (SDateLike, SDateLike) => Future[QueueJsonResponse]): Route =
    AuthByRole(ApiQueueAccess) {
      (get & path("queues")) {
        pathEnd(
          headerValueByName("X-Forwarded-Email") { email =>
            headerValueByName("X-Forwarded-Groups") { groups =>
              parameters("start", "end", "slot-size-minutes".optional) { (startStr, endStr, maybePeriodMinutes) =>
                val defaultSlotSizeMinutes = 15
                val slotSize = maybePeriodMinutes.map(_.toInt).getOrElse(defaultSlotSizeMinutes)
                val user = User.fromRoles(email, groups)
                val ports = enabledPorts.filter(user.accessiblePorts.contains(_)).toList
                val dateRangeJson = dateRangeJsonForPortsAndSlotSize(ports, slotSize)

                val start = SDate(startStr)
                val end = SDate(endStr)

                onComplete(dateRangeJson(start, end)) {
                  case Success(value) => complete(value.toJson.compactPrint)
                  case Failure(t) =>
                    log.error(s"Failed to get export: ${t.getMessage}", t)
                    complete(InternalServerError)
                }
              }
            }
          }
        )
      }
    }
}
