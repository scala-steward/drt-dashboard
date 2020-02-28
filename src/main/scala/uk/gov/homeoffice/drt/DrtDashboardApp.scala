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
import uk.gov.homeoffice.drt.DashboardClient
import uk.gov.homeoffice.drt.pages.{ Cirium, Layout }

import scala.concurrent.duration.{ Duration, _ }
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.language.postfixOps
import scala.util.{ Failure, Success }
import uk.gov.homeoffice.cirium.JsonSupport._
import uk.gov.homeoffice.cirium.services.health.CiriumAppHealthSummary

object DrtDashboardApp extends App {
  val log = Logger(getClass)

  implicit val system: ActorSystem = ActorSystem("drt-dashboard-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit lazy val timeout: Timeout = 3.seconds
  implicit val scheduler: Scheduler = system.scheduler

  lazy val routes: Route = dashRoutes ~ staticResources

  //  val portCodes = sys.env("PORT_CODES")
  //    .split(",")

  val ciriumDataUri = sys.env("CIRIUM_DATA_URI")

  lazy val dashRoutes: Route =
    pathPrefix("cirium") {
      concat(
        pathEnd {
          concat(
            get {
              complete(
                DashboardClient.get(ciriumDataUri)
                  .flatMap(res => Unmarshal[HttpResponse](res).to[CiriumAppHealthSummary])
                  .map(s => HttpEntity(ContentTypes.`text/html(UTF-8)`, Layout(Cirium(s)))))
            })
        })
    }

  val staticResources =
    (get & pathPrefix("public")) {
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

