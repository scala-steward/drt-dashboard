package uk.gov.homeoffice.drt.routes

import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.routes.Urls.{ logoutUrlForPort, portCodeFromUrl, rootDomain }

class UrlsSpec extends Specification {
  "Given a LHR DRT port url" >> {
    "When I ask for the port" >> {
      "I should get LHR" >> {
        val lhrUrl = "https://lhr." + rootDomain + "/"

        val portCode = portCodeFromUrl(lhrUrl)

        val expected = Option("LHR")

        portCode === expected
      }
    }
  }

  "Given a port code and an application domain" >> {
    "When I ask for the logout url" >> {
      "I should get port url with the logout path appended" >> {
        val logoutUrl = logoutUrlForPort("lhr")

        val expected = "https://lhr." + rootDomain + "/oauth/logout?redirect=https://lhr." + rootDomain

        logoutUrl === expected
      }
    }
  }
}
