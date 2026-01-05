package uk.gov.homeoffice.drt.services.api.v1

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import uk.gov.homeoffice.drt.models.{CrunchMinute, MinuteLike}
import uk.gov.homeoffice.drt.ports.Queues.Queue
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.ports.{PortCode, Queues}
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.{QueueJsonV1, QueueJsonResponseV1, SlotJsonV1}
import uk.gov.homeoffice.drt.time.MilliDate.MillisSinceEpoch
import uk.gov.homeoffice.drt.time.{SDate, SDateLike, UtcDate}

import scala.concurrent.{ExecutionContext, Future}

object QueueExportV1 {

  private val defaultSlotSize = 15

  def queues(queuesForPortAndDatesAndSlotSize: (PortCode, Terminal, UtcDate, UtcDate) => Source[CrunchMinute, NotUsed])
            (implicit ec: ExecutionContext, mat: Materializer): (Seq[PortCode], Int) => (SDateLike, SDateLike) => Future[QueueJsonResponseV1] =
    (portCodes, slotSize) => (start, end) => {
      if (slotSize % defaultSlotSize != 0) throw new IllegalArgumentException(s"Slot size must be a multiple of $defaultSlotSize minutes. Got $slotSize")
      val groupSize = slotSize / defaultSlotSize

      val dates = Set(start.toUtcDate, end.toUtcDate)

      Source(portCodes)
        .mapAsync(1) { portCode =>
          val terminals = AirportConfigs.confByPort(portCode).terminalsForDateRange(start.toLocalDate, end.toLocalDate)
          val eventualPortQueueSlots = terminals.map { terminal =>
              queuesForPortAndDatesAndSlotSize(portCode, terminal, dates.min, dates.max)
                .runWith(Sink.seq)
                .map { mins: Seq[CrunchMinute] =>
                  val minsInRange = mins.filter(m => start.millisSinceEpoch <= m.minute && m.minute < end.millisSinceEpoch)

                  val byMinute = terminalMinutesByMinute(minsInRange, terminal)
                  val grouped = groupCrunchMinutesBy(groupSize)(byMinute, terminal, Queues.queueOrder)
                  grouped.map {
                    case (minute, queueMinutes) =>
                      val queues = queueMinutes.map(QueueJsonV1.apply)
                      SlotJsonV1(SDate(minute), portCode, terminal, queues)
                  }
                }
            }

          Future
            .sequence(eventualPortQueueSlots)
            .map(_.flatten)
        }
        .runWith(Sink.fold(Seq.empty[SlotJsonV1])(_ ++ _))
        .map(QueueJsonResponseV1(start, end, slotSize, _))
    }

  private def terminalMinutesByMinute[T <: MinuteLike[_, _]](minutes: Seq[T],
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
    crunchMinutes
      .sortBy(_._1)
      .grouped(groupSize).toList
      .map { group =>
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
      }

}
