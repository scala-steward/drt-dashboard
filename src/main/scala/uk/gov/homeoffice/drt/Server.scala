package uk.gov.homeoffice.drt

import akka.actor.typed.scaladsl.{ ActorContext, Behaviors }
import akka.actor.typed.{ ActorSystem, Behavior, PostStop }
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives.{ concat, getFromResource, getFromResourceDirectory }
import akka.http.scaladsl.server.Route
import uk.gov.homeoffice.drt.db.{ AppDatabase, UserAccessRequestDao, UserDao }
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.ports.{ PortCode, PortRegion }
import uk.gov.homeoffice.drt.routes._
import uk.gov.homeoffice.drt.services.{ UserRequestService, UserService }

import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

case class KeyClockConfig(
  url: String,
  tokenUrl: String,
  clientId: String,
  clientSecret: String)

case class ServerConfig(
  host: String,
  port: Int,
  teamEmail: String,
  portRegions: Iterable[PortRegion],
  ciriumDataUri: String,
  rootDomain: String,
  useHttps: Boolean,
  notifyServiceApiKey: String,
  accessRequestEmails: List[String],
  neboPortCodes: Array[String],
  keyclockUrl: String,
  keyclockTokenUrl: String,
  keyclockClientId: String,
  keyclockClientSecret: String,
  keyclockUsername: String,
  keyclockPassword: String,
  scheduleFrequency: Int,
  inactivityDays: Int,
  userTrackingFeatureFlag: Boolean) {
  val portCodes: Iterable[PortCode] = portRegions.flatMap(_.ports)
  val portIataCodes: Iterable[String] = portCodes.map(_.iata)
  val clientConfig: ClientConfig = ClientConfig(portRegions, rootDomain, teamEmail)
  val keyClockConfig: KeyClockConfig = KeyClockConfig(keyclockUrl, keyclockTokenUrl, keyclockClientId, keyclockClientSecret)
}

object Server {

  sealed trait Message

  private final case class StartFailed(cause: Throwable) extends Message

  private final case class Started(binding: ServerBinding) extends Message

  case object Stop extends Message

  def apply(serverConfig: ServerConfig, notifications: EmailNotifications): Behavior[Message] = Behaviors.setup { ctx: ActorContext[Message] =>
    implicit val system: ActorSystem[Nothing] = ctx.system
    implicit val ec: ExecutionContextExecutor = system.executionContext
    val urls = Urls(serverConfig.rootDomain, serverConfig.useHttps)
    val userRequestService = new UserRequestService(new UserAccessRequestDao(AppDatabase.db, AppDatabase.userAccessRequestsTable))
    val userService = new UserService(new UserDao(AppDatabase.db, AppDatabase.userTable))
    val neboRoutes = NeboUploadRoutes(serverConfig.neboPortCodes.toList, new ProdHttpClient).route

    val routes: Route = concat(
      IndexRoute(
        urls,
        indexResource = getFromResource("frontend/index.html"),
        directoryResource = getFromResourceDirectory("frontend"),
        staticResourceDirectory = getFromResourceDirectory("frontend/static")).route,
      CiriumRoutes("cirium", serverConfig.ciriumDataUri),
      DrtRoutes("drt", serverConfig.portIataCodes),
      ApiRoutes("api", serverConfig.clientConfig, neboRoutes),
      ExportRoutes(new ProdHttpClient),
      UserRoutes("user", serverConfig.clientConfig, userService, userRequestService, notifications, serverConfig.keyclockUrl))

    val serverBinding: Future[Http.ServerBinding] = Http().newServerAt(serverConfig.host, serverConfig.port).bind(routes)

    ctx.pipeToSelf(serverBinding) {
      case Success(binding) => Started(binding)
      case Failure(ex) => StartFailed(ex)
    }

    def running(binding: ServerBinding): Behavior[Message] =
      Behaviors.receiveMessagePartial[Message] {
        case Stop =>
          ctx.log.info(
            "Stopping server http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          Behaviors.stopped
      }.receiveSignal {
        case (_, PostStop) =>
          binding.unbind()
          Behaviors.same
      }

    def starting(wasStopped: Boolean): Behaviors.Receive[Message] =
      Behaviors.receiveMessage[Message] {
        case StartFailed(cause) =>
          throw new RuntimeException("Server failed to start", cause)
        case Started(binding) =>
          ctx.log.info(
            "Server online at http://{}:{}/",
            binding.localAddress.getHostString,
            binding.localAddress.getPort)
          if (wasStopped) ctx.self ! Stop
          running(binding)
        case Stop =>
          // we got a stop message but haven't completed starting yet,
          // we cannot stop until starting has completed
          starting(wasStopped = true)
      }

    starting(wasStopped = false)
  }
}
