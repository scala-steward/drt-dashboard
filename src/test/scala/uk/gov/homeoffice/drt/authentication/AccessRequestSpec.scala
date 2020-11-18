package uk.gov.homeoffice.drt.authentication

import akka.http.scaladsl.testkit.Specs2RouteTest
import org.specs2.mutable.Specification

import spray.json._

class AccessRequestSpec extends Specification with Specs2RouteTest {
  "Given a json object string I should be able to parse it to an AccessRequest" >> {
    val string = """{"lineManager":"someone@somewhere.com","portsRequested":["bhx","lhr"],"staffing":true}"""
    import AccessRequestJsonSupport._
    val request = string.parseJson.convertTo[AccessRequest]

    val expected = AccessRequest(Set("bhx", "lhr"), staffing = true, "someone@somewhere.com")

    request === expected
  }
}
