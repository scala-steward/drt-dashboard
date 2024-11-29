package uk.gov.homeoffice.drt.services.api.v1

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.homeoffice.drt.model.CrunchMinute
import uk.gov.homeoffice.drt.ports.Queues.{EeaDesk, NonEeaDesk}
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.time.SDate

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

    val grouped = QueueExport.groupCrunchMinutesBy(2)(crunchMinutes, T1, Seq(EeaDesk, NonEeaDesk))

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
}
