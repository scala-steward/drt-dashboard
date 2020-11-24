package uk.gov.homeoffice.drt.routes

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.routes.PortUrl.{ logoutUrlForPort, portCodeFromUrl }

class PortUrlSpec extends Specification {
  "Given a LHR DRT port url" >> {
    "When I ask for the port" >> {
      "I should get LHR" >> {
        val lhrUrl = "https://lhr.drt-test.drtdomain.gov.uk/"

        val portCode = portCodeFromUrl(lhrUrl)

        val expected = "lhr"

        portCode === Option(expected)
      }
    }
  }

  "Given a port code and an application domain" >> {
    "When I ask for the logout url" >> {
      "I should get port url with the logout path appended" >> {
        val domain = "drt-test.drtdomain.gov.uk"

        val logoutUrl = logoutUrlForPort("lhr", domain)

        val expected = "https://lhr.drt-test.drtdomain.gov.uk/oauth/logout?redirect=https://lhr.drt-test.drtdomain.gov.uk"

        logoutUrl === expected
      }
    }
  }

  //https://lhr.drt-preprod.homeoffice.gov.uk/oauth/logout?redirect=https://lhr.drt-preprod.homeoffice.gov.uk/

}
