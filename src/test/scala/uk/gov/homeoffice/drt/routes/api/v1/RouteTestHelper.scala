package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.model.HttpRequest
import akka.testkit.TestProbe

import scala.concurrent.duration.DurationInt

object RouteTestHelper {
  def requestPortAndUriExist(probe: TestProbe, port: String, uri: String): Any =
    probe.fishForMessage(1.second) {
      case req: HttpRequest =>
        println(req.uri.toString)
        req.uri.toString.contains(port) && req.uri.toString.contains(uri)
    }

}
