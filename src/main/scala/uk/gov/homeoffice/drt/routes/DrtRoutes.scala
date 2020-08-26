package uk.gov.homeoffice.drt.routes

import akka.actor.{ ActorSystem, ClassicActorSystemProvider }
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity }
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import org.slf4j.{ Logger, LoggerFactory }
import uk.gov.homeoffice.drt.pages.{ Drt, Layout }
import uk.gov.homeoffice.drt.services.drt.FeedJsonSupport._
import uk.gov.homeoffice.drt.services.drt.{ DashboardPortStatus, FeedSourceStatus }
import uk.gov.homeoffice.drt.{ Dashboard, DashboardClient }

import scala.concurrent.{ ExecutionContext, Future }

object DrtRoutes {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply(prefix: String, portCodes: Array[String])(implicit system: ClassicActorSystemProvider, mat: Materializer, ec: ExecutionContext): Route = pathPrefix(prefix) {
    get {
      complete {
        eventualPortsWithStatuses(portCodes)
          .map(portsWithStatuses => HttpEntity(ContentTypes.`text/html(UTF-8)`, Layout(Drt(portsWithStatuses))))
      }
    }
  }

  private def eventualPortsWithStatuses(portCodes: Array[String])(implicit system: ClassicActorSystemProvider, mat: Materializer, ec: ExecutionContext): Future[List[DashboardPortStatus]] =
    Future
      .sequence(portCodes.map(eventualPortFeedStatuses).toList)
      .recover {
        case e: Throwable =>
          log.error("Failed to connect to DRT", e)
          List[DashboardPortStatus]()
      }

  private def eventualPortFeedStatuses(pc: String)(implicit system: ClassicActorSystemProvider, mat: Materializer, ec: ExecutionContext): Future[DashboardPortStatus] = {
    val portFeedStatusUri = Dashboard.drtUriForPortCode(pc) + "/feed-statuses"
    DashboardClient.getWithRoles(portFeedStatusUri, List(pc.toUpperCase))
      .flatMap(res => Unmarshal[HttpEntity](res.entity.withContentType(ContentTypes.`application/json`))
        .to[List[FeedSourceStatus]]
        .recover {
          case e: Throwable =>
            log.error(s"Error connecting to $pc", e)
            List[FeedSourceStatus]()
        }
        .map(portSources => DashboardPortStatus(pc, portSources)))
  }
}
