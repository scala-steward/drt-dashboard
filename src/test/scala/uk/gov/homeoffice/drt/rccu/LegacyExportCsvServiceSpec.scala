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


class LegacyExportCsvServiceSpec extends Specification {
  val testKit: ActorTestKit = ActorTestKit()
  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContextExecutor = sys.executionContext

  def exportCsvService(content: () => String): LegacyExportCsvService = LegacyExportCsvService(MockHttpClient(content))

  "Given a string of region, the service" should {
    "return the correct PortRegion" in {
      val region = exportCsvService(() => "").getPortRegion("Heathrow")
      region.get must be(PortRegion.Heathrow)
    }
  }

  "Given port code LHR I get uri for csv export for the terminal" >> {
    val expectedUri = "http://lhr:9000/export/arrivals/2022-07-22/2022-07-24/T1"
    val uri = exportCsvService(() => "").getUri(PortCode("LHR").iata, LocalDate(2022, 7, 22).toString(), LocalDate(2022, 7, 24).toString(), T1.toString)
    uri mustEqual expectedUri
  }

  "The service should prepend the region, port and terminal to each line" >> {
    "and add a new line to the end of a terminal export's last line" >> {
      exportCsvService(() => "this-is-the-last-line\n")
        .getPortResponseForTerminal(LocalDate(2022, 7, 22).toString(), LocalDate(2022, 7, 24).toString(), PortRegion.Heathrow.name, PortCode("LHR").iata, T1.toString).map { response =>
        response.utf8String mustEqual "Heathrow,LHR,T1,this-is-the-last-line\n"
      }
    }
  }
}
