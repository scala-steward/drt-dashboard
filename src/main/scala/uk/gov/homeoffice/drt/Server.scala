package uk.gov.homeoffice.drt

import akka.actor.Cancellable
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorSystem, Behavior, PostStop, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives.{concat, getFromResource, pathPrefix}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.Materializer
import akka.util.Timeout
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.db._
import uk.gov.homeoffice.drt.healthchecks._
import uk.gov.homeoffice.drt.notifications.{EmailClient, EmailNotifications, SlackClient}
import uk.gov.homeoffice.drt.persistence.{ExportPersistenceImpl, ScheduledHealthCheckPausePersistenceImpl}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.routes.{FeedbackRoutes, _}
import uk.gov.homeoffice.drt.services.s3.S3Service
import uk.gov.homeoffice.drt.services.{UserRequestService, UserService}
import uk.gov.homeoffice.drt.time.SDate
import uk.gov.homeoffice.drt.uploadTraining.FeatureGuideService

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
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
                        featureFolderPrefix: String,
                        portTerminals: Map[PortCode, Seq[Terminal]],
                        healthCheckTriggeredNotifyTemplateId: String,
                        healthCheckResolvedNotifyTemplateId: String,
                        healthCheckEmailRecipient: String,
                        healthCheckFrequencyMinutes: Int,
                        enabledPorts: Seq[PortCode],
                        slackUrl: String
                       ) {
  val portIataCodes: Iterable[String] = portTerminals.keys.map(_.iata)
  val clientConfig: ClientConfig = ClientConfig(portRegions, portTerminals, rootDomain, teamEmail)
  val keyClockConfig: KeyClockConfig = KeyClockConfig(keycloakUrl, keycloakTokenUrl, keycloakClientId, keycloakClientSecret)
}

object Server {
  private val log = LoggerFactory.getLogger(getClass)

  sealed trait Message

  private final case class StartFailed(cause: Throwable) extends Message

  private final case class Started(binding: ServerBinding) extends Message

  private case object Stop extends Message

  def apply(serverConfig: ServerConfig,
            notifications: EmailNotifications,
            emailClient: EmailClient,
            slackClient: SlackClient
           ): Behavior[Message] =
    Behaviors.setup { ctx: ActorContext[Message] =>
      implicit val system: ActorSystem[Nothing] = ctx.system
      implicit val ec: ExecutionContextExecutor = system.executionContext

      val now = () => SDate.now()

      val urls = Urls(serverConfig.rootDomain, serverConfig.useHttps)
      val userRequestService = UserRequestService(UserAccessRequestDao(ProdDatabase.db))
      val userService = UserService(UserDao(ProdDatabase.db))
      val dropInDao = DropInDao(ProdDatabase.db)
      val dropInRegistrationDao = DropInRegistrationDao(ProdDatabase.db)
      val userFeedbackDao = UserFeedbackDao(ProdDatabase.db)
      val featureGuideService = FeatureGuideService(FeatureGuideDao(ProdDatabase.db), FeatureGuideViewDao(ProdDatabase.db))

      val (exportUploader, exportDownloader) = S3Service.s3FileUploaderAndDownloader(serverConfig, serverConfig.exportsFolderPrefix)
      val (featureUploader, featureDownloader) = S3Service.s3FileUploaderAndDownloader(serverConfig, serverConfig.featureFolderPrefix)

      implicit val db: AppDatabase = ProdDatabase

      val indexRoutes = IndexRoute(urls, indexResource = getFromResource("frontend/index.html")).route

      val routes: Route = concat(
        indexRoutes,
        pathPrefix("api") {
          concat(
            PassengerRoutes(ProdHttpClient),
            CiriumRoutes(serverConfig.ciriumDataUri),
            DrtRoutes(serverConfig.portIataCodes),
            LegacyExportRoutes(ProdHttpClient, exportUploader.upload, exportDownloader.download, () => SDate.now()),
            ExportRoutes(ProdHttpClient, exportUploader.upload, exportDownloader.download, ExportPersistenceImpl(db), () => SDate.now(), emailClient, urls.rootUrl, serverConfig.teamEmail),
            UserRoutes(serverConfig.clientConfig, userService, userRequestService, notifications, serverConfig.keycloakUrl),
            FeatureGuideRoutes(featureGuideService, featureUploader, featureDownloader),
            ApiRoutes(serverConfig.clientConfig, userService, ScheduledHealthCheckPausePersistenceImpl(db, now)),
            DropInSessionsRoute(dropInDao),
            DropInRegisterRoutes(dropInRegistrationDao),
            FeedbackRoutes(userFeedbackDao),
            ExportConfigRoutes(ProdHttpClient, serverConfig.enabledPorts),
          )
        }
      )

      val serverBinding = Http().newServerAt(serverConfig.host, serverConfig.port).bind(routes)

      ctx.pipeToSelf(serverBinding) {
        case Success(binding) => Started(binding)
        case Failure(ex) => StartFailed(ex)
      }

      def running(binding: ServerBinding, monitor: Cancellable): Behavior[Message] =
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
            monitor.cancel()
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

            val monitor: Cancellable = startHealthCheckMonitor(serverConfig, slackClient, emailClient, urls, db)

            running(binding, monitor)

          case Stop =>
            // we got a stop message but haven't completed starting yet,
            // we cannot stop until starting has completed
            starting(wasStopped = true)
        }

      starting(wasStopped = false)
    }

  private def startHealthCheckMonitor(serverConfig: ServerConfig,
                                      slackClient: SlackClient,
                                      emailClient: EmailClient,
                                      urls: Urls,
                                      db: AppDatabase,
                                     )
                                     (implicit
                                      system: ActorSystem[Nothing],
                                      ec: ExecutionContext,
                                      mat: Materializer,
                                     ): Cancellable = {
    implicit val timeout: Timeout = new Timeout(1.second)
    implicit val scheduler: Scheduler = system.scheduler

    def sendSlackNotification(portCode: PortCode, checkName: String, priority: IncidentPriority, status: String): Unit = {
      val port = portCode.toString.toUpperCase
      val link = urls.urlForPort(port)
      val message = s"$port $checkName (${priority.name}) $status - $link"
      slackClient.notify(message)
    }

    val soundAlarm = (portCode: PortCode, checkName: String, priority: IncidentPriority) => {
      sendSlackNotification(portCode, checkName, priority, "triggered")
    }

    val silenceAlarm = (portCode: PortCode, checkName: String, priority: IncidentPriority) => {
      sendSlackNotification(portCode, checkName, priority, "resolved")
    }

    val healthChecksActor = system.systemActorOf(HealthChecksActor(Map.empty, soundAlarm, silenceAlarm, () => SDate.now().millisSinceEpoch, 3), "health-checks")
    val poolSettings = ConnectionPoolSettings(system)
      .withMaxConnectionBackoff(5.seconds)
      .withBaseConnectionBackoff(1.second)
      .withMaxRetries(0)
      .withMaxConnections(5)
    val makeRequest = (request: HttpRequest) => Http().singleRequest(request, settings = poolSettings)
    val recordResponse = (port: PortCode, response: HealthCheckResponse[_]) => {
      healthChecksActor.ask(replyTo => HealthChecksActor.PortHealthCheckResponse(port, response, replyTo))
    }
    log.info(s"Starting health check monitor for ports ${serverConfig.portTerminals.keys.mkString(", ")}")
    val monitor = HealthCheckMonitor(makeRequest, recordResponse, serverConfig.portTerminals.keys)
    val pausesProvider = CheckScheduledPauses.pausesProvider(ScheduledHealthCheckPausePersistenceImpl(db, () => SDate.now()))
    val pauseIsActive = CheckScheduledPauses.activePauseChecker(pausesProvider)
    object Check extends Runnable {
      override def run(): Unit = {
        pauseIsActive().foreach { paused =>
          if (paused)
            log.info("Health check monitor paused")
          else {
            log.info("Health check monitor running")
            monitor()
          }
        }
      }
    }
    scheduler.scheduleWithFixedDelay(30.seconds, serverConfig.healthCheckFrequencyMinutes.minutes)(Check)
  }
}
