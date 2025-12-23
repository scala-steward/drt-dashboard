package uk.gov.homeoffice.drt.services.api.v1_1

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.models.CrunchMinute
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Queues.{EGate, EeaDesk, NonEeaDesk}
import uk.gov.homeoffice.drt.ports.Terminals.{T1, Terminal}
import uk.gov.homeoffice.drt.routes.api.v1_1.QueueApiV1_1Routes.{QueueJsonResponseV1_1, QueueJsonV1_1, SlotJsonV1_1}
import uk.gov.homeoffice.drt.time.{SDate, SDateLike, UtcDate}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

class QueueExportTest extends AnyWordSpec with Matchers {
  "Given some 15 minutely queue slot crunch minutes, when I ask for a group size of 2 I should get 30 minutes slots" in {
    val min1 = SDate("2024-11-29T12:00")
    val min2 = SDate("2024-11-29T12:15")
    val min3 = SDate("2024-11-29T12:30")
    val min4 = SDate("2024-11-29T12:45")
    val crunchMinutes = Seq(
      min1.millisSinceEpoch -> Seq(
        CrunchMinute(T1, EeaDesk, min1.millisSinceEpoch, 5, 1, 1, 1, None, None, None, None, None, None, None),
        CrunchMinute(T1, NonEeaDesk, min1.millisSinceEpoch, 5, 1, 1, 1, None, None, None, None, None, None, None),
      ),
      min2.millisSinceEpoch -> Seq(
        CrunchMinute(T1, EeaDesk, min2.millisSinceEpoch, 5, 1, 2, 3, None, None, None, None, None, None, None),
        CrunchMinute(T1, NonEeaDesk, min2.millisSinceEpoch, 5, 1, 2, 3, None, None, None, None, None, None, None),
      ),
      min3.millisSinceEpoch -> Seq(
        CrunchMinute(T1, EeaDesk, min3.millisSinceEpoch, 5, 1, 3, 6, None, None, None, None, None, None, None),
        CrunchMinute(T1, NonEeaDesk, min3.millisSinceEpoch, 5, 1, 3, 6, None, None, None, None, None, None, None),
      ),
      min4.millisSinceEpoch -> Seq(
        CrunchMinute(T1, EeaDesk, min4.millisSinceEpoch, 5, 1, 4, 10, None, None, None, None, None, None, None),
        CrunchMinute(T1, NonEeaDesk, min4.millisSinceEpoch, 5, 1, 4, 10, None, None, None, None, None, None, None),
      ),
    )

    val grouped = QueueExportV1_1.groupCrunchMinutesBy(2)(crunchMinutes, T1, Seq(EeaDesk, NonEeaDesk))

    grouped should ===(Seq(
      min1.millisSinceEpoch -> Seq(
        CrunchMinute(T1, EeaDesk, min1.millisSinceEpoch, 10, 2, 2, 3, None, Some(0), Some(0), Some(0), None, None, None),
        CrunchMinute(T1, NonEeaDesk, min1.millisSinceEpoch, 10, 2, 2, 3, None, Some(0), Some(0), Some(0), None, None, None),
      ),
      min3.millisSinceEpoch -> Seq(
        CrunchMinute(T1, EeaDesk, min3.millisSinceEpoch, 10, 2, 4, 10, None, Some(0), Some(0), Some(0), None, None, None),
        CrunchMinute(T1, NonEeaDesk, min3.millisSinceEpoch, 10, 2, 4, 10, None, Some(0), Some(0), Some(0), None, None, None),
      ),
    ))
  }


  val start: SDateLike = SDate("2024-10-15T12:00")
  val end: SDateLike = SDate("2024-10-15T12:30")
  val utcDate: UtcDate = start.toUtcDate

  "QueueExport" should {
    "return a PortQueuesJson with the correct structure and only the values in the requested time range" in {
      val system = ActorSystem("QueueExportSpec")
      implicit val mat: Materializer = Materializer(system)

      val source: (PortCode, Terminal, UtcDate, UtcDate) => Source[CrunchMinute, NotUsed] = (_: PortCode, _: Terminal, _: UtcDate, _: UtcDate) => {
        Source(List(
          CrunchMinute(T1, EeaDesk, start.addMinutes(-15).millisSinceEpoch, 10d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, NonEeaDesk, start.addMinutes(-15).millisSinceEpoch, 12d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, EGate, start.addMinutes(-15).millisSinceEpoch, 14d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, EeaDesk, start.millisSinceEpoch, 10d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, NonEeaDesk, start.millisSinceEpoch, 12d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, EGate, start.millisSinceEpoch, 14d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, EeaDesk, start.addMinutes(15).millisSinceEpoch, 10d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, NonEeaDesk, start.addMinutes(15).millisSinceEpoch, 12d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, EGate, start.addMinutes(15).millisSinceEpoch, 14d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, EeaDesk, start.addMinutes(30).millisSinceEpoch, 10d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, NonEeaDesk, start.addMinutes(30).millisSinceEpoch, 12d, 0d, 0, 0, None, None, None, None, None, None, None),
          CrunchMinute(T1, EGate, start.addMinutes(30).millisSinceEpoch, 14d, 0d, 0, 0, None, None, None, None, None, None, None),
        ))
      }
      val export = QueueExportV1_1.queues(source)
      Await.result(export(Seq(PortCode("STN")), 15)(start, end), 1.second) should ===(
        QueueJsonResponseV1_1(
          start,
          end,
          15,
          Seq(
            SlotJsonV1_1(start, PortCode("STN"), T1,
              Seq(
                QueueJsonV1_1(EeaDesk, 10, 0),
                QueueJsonV1_1(EGate, 14, 0),
                QueueJsonV1_1(NonEeaDesk, 12, 0),
              )),
            SlotJsonV1_1(start.addMinutes(15), PortCode("STN"), T1,
              Seq(
                QueueJsonV1_1(EeaDesk, 10, 0),
                QueueJsonV1_1(EGate, 14, 0),
                QueueJsonV1_1(NonEeaDesk, 12, 0),
              ))
          )
        )
      )
    }
  }
}
