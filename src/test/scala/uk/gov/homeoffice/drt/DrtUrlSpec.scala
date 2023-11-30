package uk.gov.homeoffice.drt

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.Dashboard.drtInternalUriForPortCode
import uk.gov.homeoffice.drt.ports.PortCode

class DrtUrlSpec extends Specification {

  "Given a port code and an endpoint I should get back an internal url that will work in kubernetes" >> {
    val portCode = PortCode("lhr")

    val result = drtInternalUriForPortCode(portCode)

    val expected = "http://lhr:9000"

    result === expected
  }
}
