package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.server.Directives.{ concat, getFromResource, getFromResourceDirectory, path, pathPrefix }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get

object IndexRoute {
  def apply(): Route = {
    concat(
      path("") {
        getFromResource("frontend/index.html")
      },
      (get & pathPrefix("")) {
        getFromResourceDirectory("./frontend")
      },
      (get & pathPrefix("static")) {
        getFromResourceDirectory("./frontend/static")
      })
  }
}
