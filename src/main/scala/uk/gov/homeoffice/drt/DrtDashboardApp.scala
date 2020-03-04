package uk.gov.homeoffice

import akka.actor.{ ActorSystem, Scheduler }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.scalalogging.Logger
import uk.gov.homeoffice.cirium.services.health.CiriumAppHealthSummary
import uk.gov.homeoffice.drt.pages.{ Cirium, Drt, Error, Layout }
import uk.gov.homeoffice.drt.services.drt.JsonSupport._
import uk.gov.homeoffice.drt.services.drt.{ DashboardPortStatus, FeedSourceStatus }
import uk.gov.homeoffice.drt.{ Dashboard, DashboardClient }

import scala.concurrent.duration.{ Duration, _ }
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success }

object DrtDashboardApp extends App {
  val log = Logger(getClass)

  implicit val system: ActorSystem = ActorSystem("drt-dashboard-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit lazy val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = system.scheduler

  lazy val routes: Route = dashRoutes ~ staticResources

  val portCodes = sys.env("PORT_CODES")
    .split(",")

  val ciriumDataUri = sys.env("CIRIUM_DATA_URI")

  lazy val dashRoutes: Route =
    concat(
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
      },
      pathPrefix("drt") {
        get {
          complete {
            Future.sequence(portCodes.map(
              pc => {
                val portFeedStatus = Dashboard.drtUriForPortCode(pc) + "/feed-statuses"
                DashboardClient.getWithRoles(portFeedStatus, List(pc.toUpperCase))
                  .flatMap(res => Unmarshal[HttpEntity](res.entity.withContentType(ContentTypes.`application/json`))
                    .to[List[FeedSourceStatus]].map(portSources => pc -> portSources))
              }).toList)
              .recover {
                case e: Throwable =>
                  log.error("Failed to connect to DRT", e)
                  List[(String, List[FeedSourceStatus])]()
              }
              .map(_.map {
                case (portCode, feedsStatus) => DashboardPortStatus(portCode, feedsStatus)
              })
              .map(ps => HttpEntity(ContentTypes.`text/html(UTF-8)`, Layout(Drt(ps.toList))))
          }
        }
      })

  val staticResources = (get & pathPrefix("public")) {
    {
      getFromResourceDirectory("public")
    }
  }

  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "0.0.0.0", 8081)

  serverBinding.onComplete {
    case Success(bound) =>
      log.info(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) =>
      log.error(s"Server could not start!", e)
      system.terminate()
  }
  Await.result(system.whenTerminated, Duration.Inf)
}

