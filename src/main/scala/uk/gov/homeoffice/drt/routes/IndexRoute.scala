package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.server.Directives.{ getFromDirectory, pathPrefix, concat, path }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get

object IndexRoute {
  def apply(): Route = {
    concat(
      path("") {
        getFromDirectory("./react/build/index.html")
      },
      (get & pathPrefix("")) {
        getFromDirectory("./react/build")
      },
      (get & pathPrefix("static")) {
        getFromDirectory("./react/build/static")
      })
  }
}
