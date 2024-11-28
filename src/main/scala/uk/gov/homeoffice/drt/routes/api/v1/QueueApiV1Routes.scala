package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import spray.json._
import uk.gov.homeoffice.drt.auth.Roles.{ApiFlightAccess, ApiQueueAccess}
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.db.AppDatabase
import uk.gov.homeoffice.drt.db.dao.QueueSlotDao
import uk.gov.homeoffice.drt.model.{CrunchMinute, MinuteLike}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.ports.{PortCode, Queues}
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.QueueJsonResponse
import uk.gov.homeoffice.drt.routes.api.v1.QueueExport.{PeriodJson, PortQueuesJson, QueueJson, TerminalQueuesJson}
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time.{SDate, SDateLike, UtcDate}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait QueueApiV1JsonFormats extends DefaultJsonProtocol {

  implicit object QueueJsonFormat extends RootJsonFormat[Queue] {
    override def write(obj: Queue): JsValue = obj.stringValue.toJson

    override def read(json: JsValue): Queue = json match {
      case JsString(value) => Queue(value)
      case unexpected => throw new Exception(s"Failed to parse Queue. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val queueJsonFormat: RootJsonFormat[QueueJson] = jsonFormat3(QueueJson.apply)

  implicit object SDateJsonFormat extends RootJsonFormat[SDateLike] {
    override def write(obj: SDateLike): JsValue = obj.toISOString.toJson

    override def read(json: JsValue): SDateLike = json match {
      case JsString(value) => SDate(value)
      case unexpected => throw new Exception(s"Failed to parse SDate. Expected JsNumber. Got ${unexpected.getClass}")
    }
  }

  implicit val periodJsonFormat: RootJsonFormat[PeriodJson] = jsonFormat2(PeriodJson.apply)

  implicit object TerminalJsonFormat extends RootJsonFormat[Terminal] {
    override def write(obj: Terminal): JsValue = obj.toString.toJson

    override def read(json: JsValue): Terminal = json match {
      case JsString(value) => Terminal(value)
      case unexpected => throw new Exception(s"Failed to parse Terminal. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val terminalQueuesJsonFormat: RootJsonFormat[TerminalQueuesJson] = jsonFormat2(TerminalQueuesJson.apply)

  implicit object PortCodeJsonFormat extends RootJsonFormat[PortCode] {
    override def write(obj: PortCode): JsValue = obj.iata.toJson

    override def read(json: JsValue): PortCode = json match {
      case JsString(value) => PortCode(value)
      case unexpected => throw new Exception(s"Failed to parse Terminal. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val portQueuesJsonFormat: RootJsonFormat[PortQueuesJson] = jsonFormat2(PortQueuesJson.apply)

  implicit object jsonResponseFormat extends RootJsonFormat[QueueJsonResponse] {

    override def write(obj: QueueJsonResponse): JsValue = obj match {
      case obj: QueueJsonResponse => JsObject(Map(
        "startTime" -> obj.startTime.toJson,
        "endTime" -> obj.endTime.toJson,
        "periodLengthMinutes" -> obj.slotSizeMinutes.toJson,
        "ports" -> obj.ports.toJson,
      ))
    }

    override def read(json: JsValue): QueueJsonResponse = throw new Exception("Not implemented")
  }
}



object QueueApiV1Routes extends DefaultJsonProtocol with QueueApiV1JsonFormats {
  private val log = LoggerFactory.getLogger(getClass)

  case class QueueJsonResponse(startTime: String, endTime: String, slotSizeMinutes: Int, ports: Seq[PortQueuesJson])

  def apply(enabledPorts: Iterable[PortCode],
            arrivalSource: (Seq[PortCode], Int) => (SDateLike, SDateLike) => Future[QueueJsonResponse]): Route =
    AuthByRole(ApiQueueAccess) {
      (get & path("queues")) {
        pathEnd(
          headerValueByName("X-Forwarded-Email") { email =>
            headerValueByName("X-Forwarded-Groups") { groups =>
              parameters("start", "end", "period-minutes".optional) { (startStr, endStr, maybePeriodMinutes) =>
                val defaultSlotSizeMinutes = 15
                val slotSize = maybePeriodMinutes.map(_.toInt).getOrElse(defaultSlotSizeMinutes)
                val user = User.fromRoles(email, groups)
                val ports = enabledPorts.filter(user.accessiblePorts.contains(_)).toList
                val queuesJson = arrivalSource(ports, slotSize)

                val start = SDate(startStr)
                val end = SDate(endStr)

                onComplete(queuesJson(start, end)) {
                  case Success(value) => complete(value.toJson.compactPrint)
                  case Failure(t) =>
                    log.error(s"Failed to get export: ${t.getMessage}")
                    complete(InternalServerError)
                }
              }
            }
          }
        )
      }
    }
}

object QueueExport {

  case class QueueJson(queue: Queue, incomingPax: Int, maxWaitMinutes: Int)

  object QueueJson {
    def apply(cm: CrunchMinute): QueueJson = QueueJson(cm.queue, cm.paxLoad.toInt, cm.waitTime)
  }

  case class PeriodJson(startTime: SDateLike, queues: Iterable[QueueJson])

  case class TerminalQueuesJson(terminal: Terminal, periods: Iterable[PeriodJson])

  case class PortQueuesJson(portCode: PortCode, terminals: Iterable[TerminalQueuesJson])

  def queues(db: AppDatabase)
            (implicit ec: ExecutionContext, mat: Materializer): (Seq[PortCode], Int) => (SDateLike, SDateLike) => Future[QueueJsonResponse] =
    (portCodes, slotSize) => (start, end) => {
      if (slotSize % 15 != 0) throw new IllegalArgumentException(s"Slot size must be a multiple of 15 minutes. Got $slotSize")
      val groupSize = slotSize / 15

      val dates = Set(start.toLocalDate, end.toLocalDate)
      val dao = QueueSlotDao()

      Source(portCodes)
        .mapAsync(1) { portCode =>
          val eventualPortQueueSlots = AirportConfigs.confByPort(portCode).terminals.map { terminal =>
            val queueSlotsForDatesAndTerminals = dao.queueSlotsForDateRange(portCode, db.run)

            queueSlotsForDatesAndTerminals(dates.min, dates.max, Seq(terminal))
              .runWith(Sink.seq)
              .map { r: Seq[(UtcDate, Seq[CrunchMinute])] =>
                val periodJsons = r.flatMap {
                  case (_, mins) =>
                    val byMinute = terminalMinutesByMinute(mins, terminal)
                    val grouped = groupCrunchMinutesBy(groupSize)(byMinute, terminal, Queues.queueOrder)
                    grouped.map {
                      case (minute, queueMinutes) =>
                        val queues = queueMinutes.map(QueueJson.apply)
                        PeriodJson(SDate(minute), queues)
                    }
                }
                TerminalQueuesJson(terminal, periodJsons)
              }
          }

          Future
            .sequence(eventualPortQueueSlots)
            .map(PortQueuesJson(portCode, _))
        }
        .runWith(Sink.seq)
        .map(QueueJsonResponse(start.toString(), end.toString(), slotSize, _))
    }

  def terminalMinutesByMinute[T <: MinuteLike[_, _]](minutes: Seq[T],
                                                     terminalName: Terminal): Seq[(MillisSinceEpoch, Seq[T])] =
    minutes
      .filter(_.terminal == terminalName)
      .groupBy(_.minute)
      .toList
      .sortBy(_._1)


  def groupCrunchMinutesBy(groupSize: Int)
                          (crunchMinutes: Seq[(MillisSinceEpoch, Seq[CrunchMinute])],
                           terminalName: Terminal,
                           queueOrder: Seq[Queue],
                          ): Seq[(MillisSinceEpoch, Seq[CrunchMinute])] =
    crunchMinutes.grouped(groupSize).toList.map(group => {
      val byQueueName = group.flatMap(_._2).groupBy(_.queue)
      val startMinute = group.map(_._1).min
      val queueCrunchMinutes = queueOrder.collect {
        case qn if byQueueName.contains(qn) =>
          val queueMinutes: Seq[CrunchMinute] = byQueueName(qn)
          val allActDesks = queueMinutes.collect {
            case cm: CrunchMinute if cm.actDesks.isDefined => cm.actDesks.getOrElse(0)
          }
          val actDesks = if (allActDesks.isEmpty) None else Option(allActDesks.max)
          val allActWaits = queueMinutes.collect {
            case cm: CrunchMinute if cm.actWait.isDefined => cm.actWait.getOrElse(0)
          }
          val actWaits = if (allActWaits.isEmpty) None else Option(allActWaits.max)
          CrunchMinute(
            terminal = terminalName,
            queue = qn,
            minute = startMinute,
            paxLoad = queueMinutes.map(_.paxLoad).sum,
            workLoad = queueMinutes.map(_.workLoad).sum,
            deskRec = queueMinutes.map(_.deskRec).max,
            waitTime = queueMinutes.map(_.waitTime).max,
            maybePaxInQueue = queueMinutes.map(_.maybePaxInQueue).max,
            deployedDesks = Option(queueMinutes.map(_.deployedDesks.getOrElse(0)).max),
            deployedWait = Option(queueMinutes.map(_.deployedWait.getOrElse(0)).max),
            maybeDeployedPaxInQueue = Option(queueMinutes.map(_.maybeDeployedPaxInQueue.getOrElse(0)).max),
            actDesks = actDesks,
            actWait = actWaits
          )
      }
      (startMinute, queueCrunchMinutes)
    })

}
