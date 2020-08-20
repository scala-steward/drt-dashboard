package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, StatusCodes }
import akka.http.scaladsl.server.Directives.{ complete, parameters, pathPrefix, _ }
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.authentication.Roles
import uk.gov.homeoffice.drt.authentication.Roles.{ PortAccess, Role }

object LoginRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(prefix: String, portCodes: Array[String]): Route =
    pathPrefix(prefix) {
      get { context =>
        //        parameters("port".optional) { maybePort =>
        //          val roles = context.request.headers.find(_.name.toLowerCase == "x-auth-roles").map { header =>
        //            header.value.split(",").flatMap(Roles.parse).toSet
        //          }.getOrElse(Set.empty[Role])
        //
        //          if (roles.isEmpty)
        //            context.complete("Welcome new user. Choose the ports you'd like access to")
        //          else {
        //            maybePort match {
        //              //              case Some(port) if roles.contains(Roles.parse(port)) => redirect(s"$port.drt-preprod.homeoffice.gov.uk", StatusCodes.Redirection)
        //              case None =>
        //                context.complete(s"Welcome existing user. Please choose a port from ${roles.collect { case r: PortAccess => r }.mkString(", ")}")
        //            }
        //          }
        //        }
        context.complete("yeah")
      }
    }
}
