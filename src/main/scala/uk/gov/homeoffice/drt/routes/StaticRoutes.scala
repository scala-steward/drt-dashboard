package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.server.Directives.{ getFromResourceDirectory, pathPrefix }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get

object StaticRoutes {
  def apply(prefix: String): Route = (get & pathPrefix(prefix)) {
    {
      getFromResourceDirectory("public")
    }
  }
}
