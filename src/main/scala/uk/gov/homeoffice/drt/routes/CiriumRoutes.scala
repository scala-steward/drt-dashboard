package uk.gov.homeoffice.drt.routes

import akka.actor.{ActorSystem, ClassicActorSystemProvider}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives.{pathEnd, pathPrefix}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import org.slf4j.{Logger, LoggerFactory}
import uk.gov.homeoffice.cirium.services.health.CiriumAppHealthSummary
import uk.gov.homeoffice.drt.DashboardClient
import uk.gov.homeoffice.drt.pages.{Cirium, Error, Layout}

import scala.concurrent.ExecutionContext

object CiriumRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(ciriumDataUri: String)
           (implicit system: ClassicActorSystemProvider, mat: Materializer, ec: ExecutionContext): Route =
    pathPrefix("cirium") {
      pathEnd {
        get {
          import uk.gov.homeoffice.cirium.JsonSupport._
          complete(
            DashboardClient.get(ciriumDataUri)
              .flatMap(res => Unmarshal[HttpResponse](res).to[CiriumAppHealthSummary])
              .map(s => Layout(Cirium(s)))
              .recover {
                case e: Throwable =>
                  log.error("Unable to connect to Cirium Feed", e)
                  Layout(Error("Unable to connect to Cirium Feed"))
              }
              .map(page => HttpEntity(ContentTypes.`text/html(UTF-8)`, page)))
        }
      }
    }
}
