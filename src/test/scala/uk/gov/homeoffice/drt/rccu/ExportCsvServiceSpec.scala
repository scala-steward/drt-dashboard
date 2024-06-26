package uk.gov.homeoffice.drt.rccu

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.MockHttpClient
import uk.gov.homeoffice.drt.exports._
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.{T1, T2}
import uk.gov.homeoffice.drt.time.LocalDate

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor}


class ExportCsvServiceSpec extends Specification {
  val testKit: ActorTestKit = ActorTestKit()
  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContextExecutor = sys.executionContext

  def exportCsvService(content: () => String): ExportCsvService = ExportCsvService(MockHttpClient(content))

  "Given port code LHR I get uri for csv export for the terminal" >> {
    "For an arrivals export" >> {
      val expectedUri = "http://lhr:9000/api/arrivals/2022-07-22/2022-07-24/T1?granularity=total"
      val uri = ExportCsvService.getUri(Arrivals, LocalDate(2022, 7, 22), LocalDate(2022, 7, 24), PortCode("LHR"), Option(T1))
      uri mustEqual expectedUri
    }
    "For a port export" >> {
      val expectedUri = "http://lhr:9000/api/passengers/2022-07-22/2022-07-24?granularity=total"
      val uri = ExportCsvService.getUri(PortPassengers, LocalDate(2022, 7, 22), LocalDate(2022, 7, 24), PortCode("LHR"), None)
      uri mustEqual expectedUri
    }
    "For a daily port export" >> {
      val expectedUri = "http://lhr:9000/api/passengers/2022-07-22/2022-07-24?granularity=daily"
      val uri = ExportCsvService.getUri(PortPassengersDaily, LocalDate(2022, 7, 22), LocalDate(2022, 7, 24), PortCode("LHR"), None)
      uri mustEqual expectedUri
    }
    "For a terminal export" >> {
      val expectedUri = "http://lhr:9000/api/passengers/2022-07-22/2022-07-24/T2?granularity=total"
      val uri = ExportCsvService.getUri(TerminalPassengers, LocalDate(2022, 7, 22), LocalDate(2022, 7, 24), PortCode("LHR"), Option(T2))
      uri mustEqual expectedUri
    }
    "For a daily terminal export" >> {
      val expectedUri = "http://lhr:9000/api/passengers/2022-07-22/2022-07-24/T2?granularity=daily"
      val uri = ExportCsvService.getUri(TerminalPassengersDaily, LocalDate(2022, 7, 22), LocalDate(2022, 7, 24), PortCode("LHR"), Option(T2))
      uri mustEqual expectedUri
    }
  }

  "responseContentAsByteString should" >> {
    "Return a string given a non-empty response" >> {
      val service = ExportCsvService(MockHttpClient(() => "content"))
      val byteString = Await.result(service.responseContentAsByteString("uri", PortCode("LHR")), 1.second)

      byteString.utf8String mustEqual "content"
    }
    "Return an empty string given an empty response" >> {
      val service = ExportCsvService(MockHttpClient(() => ""))
      val byteString = Await.result(service.responseContentAsByteString("uri", PortCode("LHR")), 1.second)

      byteString.utf8String mustEqual ""
    }
    "Throw an exception given a non-200 response" >> {
      val service = ExportCsvService(MockHttpClient(() => throw new Exception("boom")))
      Await.result(service.responseContentAsByteString("uri", PortCode("LHR")), 1.second) must throwA[Exception]
    }
  }
}
