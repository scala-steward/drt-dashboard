package uk.gov.homeoffice.drt.rccu

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.MockHttpClient
import uk.gov.homeoffice.drt.exports.Arrivals
import uk.gov.homeoffice.drt.ports.Terminals.T1
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.time.LocalDate

import scala.concurrent.ExecutionContextExecutor


class ExportCsvServiceSpec extends Specification {
  val testKit: ActorTestKit = ActorTestKit()
  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContextExecutor = sys.executionContext

  def exportCsvService(content: () => String): ExportCsvService = ExportCsvService(MockHttpClient(content))

  "Given a string of region, the service" should {
    "return the correct PortRegion" in {
      val region = exportCsvService(() => "").getPortRegion("Heathrow")
      region.get must be(PortRegion.Heathrow)
    }
  }

  "Given port code LHR I get uri for csv export for the terminal" >> {
    val expectedUri = "http://lhr:9000/arrivals/2022-07-22/2022-07-24/T1"
    val uri = exportCsvService(() => "").getUri(Arrivals, LocalDate(2022, 7, 22), LocalDate(2022, 7, 24), PortCode("LHR"), T1)
    uri mustEqual expectedUri
  }

  "The service should prepend the region, port and terminal to each line" >> {
    "and add a new line to the end of a terminal export's last line" >> {
      exportCsvService(() => "this-is-the-last-line\n")
        .getPortResponseForTerminal(Arrivals, LocalDate(2022, 7, 22), LocalDate(2022, 7, 24), PortCode("LHR"), T1).map { response =>
        response.utf8String mustEqual "this-is-the-last-line\n"
      }
    }
  }
}
