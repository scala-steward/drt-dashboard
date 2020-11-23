package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.server.Directives.{ concat, getFromResource, getFromResourceDirectory, optionalHeaderValueByName, path, pathPrefix }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }

object IndexRoute {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(): Route = {
    concat(
      path("") { r =>
        //        optionalHeaderValueByName("Referer") {
        //          case Some(referer) =>
        //            log.info(s"Referer: $referer")
        //            getFromResource("frontend/index.html")
        //          case _ =>
        //            getFromResource("frontend/index.html")
        r.request.headers.foreach(h => log.info(s"header: ${h.name()}: ${h.value()}"))
        r.complete("yeah")
        //        }
      },
      (get & pathPrefix("")) { r =>
        r.request.headers.foreach(h => log.info(s"header: ${h.name()}: ${h.value()}"))
        r.complete("yeah")
        //        getFromResourceDirectory("frontend")
      },
      (get & pathPrefix("static")) {
        getFromResourceDirectory("frontend/static")
      })
  }
}
