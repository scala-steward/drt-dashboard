package uk.gov.homeoffice.drt.routes

import org.apache.pekko.actor.{ActorSystem, ClassicActorSystemProvider}
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import org.apache.pekko.http.scaladsl.server.Directives.{pathEnd, pathPrefix}
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.directives.MethodDirectives.get
import org.apache.pekko.http.scaladsl.server.directives.RouteDirectives.complete
import org.apache.pekko.http.scaladsl.unmarshalling.Unmarshal
import org.apache.pekko.stream.Materializer
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
