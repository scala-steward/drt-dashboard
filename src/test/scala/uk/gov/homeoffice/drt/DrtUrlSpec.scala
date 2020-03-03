package uk.gov.homeoffice.drt

import org.specs2.mutable.Specification
import Dashboard.drtUriForPortCode

class DrtUrlSpec extends Specification {

  "Given a port code and an endpoint I should get back an internal url that will work in kubernetes" >> {
    val portCode = "lhr"

    val result = drtUriForPortCode(portCode)

    val expected = "http://lhr:9000"

    result === expected
  }
  
}
