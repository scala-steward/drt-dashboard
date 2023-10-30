package uk.gov.homeoffice.drt

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, PostStop}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives.{concat, getFromResource, getFromResourceDirectory}
import akka.http.scaladsl.server.Route
import uk.gov.homeoffice.drt.db._
import uk.gov.homeoffice.drt.notifications.{EmailClient, EmailNotifications}
import uk.gov.homeoffice.drt.persistence.ExportPersistenceImpl
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.routes._
import uk.gov.homeoffice.drt.services.s3.S3Service
import uk.gov.homeoffice.drt.services.{UserRequestService, UserService}
import uk.gov.homeoffice.drt.time.SDate
import uk.gov.homeoffice.drt.uploadTraining.FeatureGuideService

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class KeyClockConfig(url: String,
                          tokenUrl: String,
                          clientId: String,
                          clientSecret: String)

case class ServerConfig(host: String,
                        port: Int,
                        teamEmail: String,
                        portRegions: Iterable[PortRegion],
                        ciriumDataUri: String,
                        rootDomain: String,
                        useHttps: Boolean,
                        notifyServiceApiKey: String,
                        accessRequestEmails: List[String],
                        neboPortCodes: Array[String],
                        keycloakUrl: String,
                        keycloakTokenUrl: String,
                        keycloakClientId: String,
                        keycloakClientSecret: String,
                        keycloakUsername: String,
                        keycloakPassword: String,
                        dormantUsersCheckFrequency: Int,
                        dropInRemindersCheckFrequency: Int,
                        dropInNotificationFrequency: Int,
                        inactivityDays: Int,
                        deactivateAfterWarningDays: Int,
                        userTrackingFeatureFlag: Boolean,
                        s3AccessKey: String,
                        s3SecretAccessKey: String,
                        drtS3BucketName: String,
                        exportsFolderPrefix: String,
                        featureFolderPrefix: String
                       ) {
  val portCodes: Iterable[PortCode] = portRegions.flatMap(_.ports)
  val portIataCodes: Iterable[String] = portCodes.map(_.iata)
  val clientConfig: ClientConfig = ClientConfig(portRegions, rootDomain, teamEmail)
  val keyClockConfig: KeyClockConfig = KeyClockConfig(keycloakUrl, keycloakTokenUrl, keycloakClientId, keycloakClientSecret)
}

object Server {
  sealed trait Message

  private final case class StartFailed(cause: Throwable) extends Message

  private final case class Started(binding: ServerBinding) extends Message

  private case object Stop extends Message

  def apply(serverConfig: ServerConfig,
            notifications: EmailNotifications,
            emailClient: EmailClient,
           ): Behavior[Message] =
    Behaviors.setup { ctx: ActorContext[Message] =>
      implicit val system: ActorSystem[Nothing] = ctx.system
      implicit val ec: ExecutionContextExecutor = system.executionContext
      val urls = Urls(serverConfig.rootDomain, serverConfig.useHttps)
      val userRequestService = UserRequestService(UserAccessRequestDao(ProdDatabase.db))
      val userService = UserService(UserDao(ProdDatabase.db))
      val dropInDao = DropInDao(ProdDatabase.db)
      val dropInRegistrationDao = DropInRegistrationDao(ProdDatabase.db)

      val featureGuideService = FeatureGuideService(FeatureGuideDao(ProdDatabase.db), FeatureGuideViewDao(ProdDatabase.db))
      val neboRoutes = NeboUploadRoutes(serverConfig.neboPortCodes.toList, ProdHttpClient).route

      val (exportUploader, exportDownloader) = S3Service.s3FileUploaderAndDownloader(serverConfig, serverConfig.exportsFolderPrefix)
      val (featureUploader, featureDownloader) = S3Service.s3FileUploaderAndDownloader(serverConfig, serverConfig.featureFolderPrefix)
      implicit val db: ProdDatabase.type = ProdDatabase

      val routes: Route = concat(
        IndexRoute(
          urls,
          indexResource = getFromResource("frontend/index.html"),
          directoryResource = getFromResourceDirectory("frontend"),
          staticResourceDirectory = getFromResourceDirectory("frontend/static")).route,
        CiriumRoutes("cirium", serverConfig.ciriumDataUri),
        DrtRoutes("drt", serverConfig.portIataCodes),
        ApiRoutes("api", serverConfig.clientConfig, neboRoutes, userService),
        LegacyExportRoutes(ProdHttpClient, exportUploader.upload, exportDownloader.download, () => SDate.now()),
        ExportRoutes(ProdHttpClient, exportUploader.upload, exportDownloader.download, ExportPersistenceImpl(db), () => SDate.now(), emailClient, urls.rootUrl, serverConfig.teamEmail),
        UserRoutes("user", serverConfig.clientConfig, userService, userRequestService, notifications, serverConfig.keycloakUrl),
        FeatureGuideRoutes("guide", featureGuideService, featureUploader, featureDownloader),
        DropInRoute("drop-in",dropInDao),
        DropInRegisterRoutes("drop-in-register",dropInRegistrationDao)
      )

      val serverBinding: Future[ServerBinding] = Http().newServerAt(serverConfig.host, serverConfig.port).bind(routes)

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
