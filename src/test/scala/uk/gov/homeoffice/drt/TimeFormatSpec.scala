package uk.gov.homeoffice.drt

import org.joda.time.DateTime
import org.specs2.mutable.Specification

class TimeFormatSpec extends Specification {
  import Dashboard.timeAgoInWords

  val baseDateFunc = () => new DateTime(2020, 3, 3, 13, 0, 0, 0).getMillis

  "When formatting time ago in words" >> {

    "Given 0 millis the the result should be 0 seconds" >> {
      val zero = 0L
      val result = timeAgoInWords(zero, baseDateFunc)
      val expected = "0 seconds"

      result === expected
    }

    val oneSecondMillis = 1000L
    "Given 1000 millis the the result should be 0 seconds" >> {
      val zero = oneSecondMillis
      val result = timeAgoInWords(zero, baseDateFunc)
      val expected = "1 second"

      result === expected
    }

    "Given 5 minutes in millis the the result should be 5 minutes" >> {
      val fiveMinutes = 60 * 5 * oneSecondMillis
      val result = timeAgoInWords(fiveMinutes, baseDateFunc)
      val expected = "5 minutes"

      result === expected
    }

    "Given 5 minutes in millis the the result should be 5 minutes" >> {
      val fiveMinutes = 60 * 5 * oneSecondMillis
      val result = timeAgoInWords(fiveMinutes, baseDateFunc)
      val expected = "5 minutes"

      result === expected
    }

    "Given 5 minutes 30 seconds the the result should be 5 minutes 30 seconds" >> {
      val fiveMinutesThirtySeconds = (60 * 5 * oneSecondMillis) + 30000L
      val result = timeAgoInWords(fiveMinutesThirtySeconds, baseDateFunc)
      val expected = "5 minutes 30 seconds"

      result === expected
    }

    val oneHourMillis = 60 * 60 * oneSecondMillis
    "Given 1 hour the the result should be 1 hour" >> {
      val oneHour = oneHourMillis
      val result = timeAgoInWords(oneHour, baseDateFunc)
      val expected = "1 hour"

      result === expected
    }

    "Given 1 hour and 1 second the the result should be 1 hour" >> {
      val oneHourAndOneSecond = oneHourMillis + oneSecondMillis
      val result = timeAgoInWords(oneHourAndOneSecond, baseDateFunc)
      val expected = "1 hour"

      result === expected
    }

    "Given 2 hours and 1 minute the the result should be 2 hours" >> {
      val oneHourAndOneSecond = (oneHourMillis * 2) + oneSecondMillis
      val result = timeAgoInWords(oneHourAndOneSecond, baseDateFunc)
      val expected = "2 hours"

      result === expected
    }

    "Given 1 day and 1 hour the the result should be 1 day" >> {
      val oneHourAndOneSecond = (oneHourMillis * 25)
      val result = timeAgoInWords(oneHourAndOneSecond, baseDateFunc)
      val expected = "1 day"

      result === expected
    }

    val oneDayMillis = oneHourMillis * 24

    val oneWeekMillis = oneDayMillis * 7
    "Given 1 week and 1 day the the result should be 1 week and 1 day" >> {
      val oneHourAndOneSecond = oneWeekMillis + oneDayMillis
      val result = timeAgoInWords(oneHourAndOneSecond, baseDateFunc)
      val expected = "1 week 1 day"

      result === expected
    }

    "Given 6 weeks and 1 day the the result should be 6 weeks" >> {
      val oneHourAndOneSecond = oneWeekMillis * 6 + oneDayMillis
      val result = timeAgoInWords(oneHourAndOneSecond, baseDateFunc)
      val expected = "1 month 1 week"

      result === expected
    }
  }

}
