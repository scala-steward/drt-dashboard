package uk.gov.homeoffice.drt

import akka.NotUsed
import akka.actor.Cancellable
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Directives.{concat, getFromResource, pathPrefix}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.db._
import uk.gov.homeoffice.drt.db.dao.{QueueSlotDao, UserFeedbackDao}
import uk.gov.homeoffice.drt.healthchecks._
import uk.gov.homeoffice.drt.keycloak.KeyCloakAuth
import uk.gov.homeoffice.drt.model.CrunchMinute
import uk.gov.homeoffice.drt.notifications._
import uk.gov.homeoffice.drt.persistence.{ExportPersistenceImpl, ScheduledHealthCheckPausePersistenceImpl}
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports._
import uk.gov.homeoffice.drt.routes._
import uk.gov.homeoffice.drt.routes.api.v1.{AuthApiV1Routes, FlightApiV1Routes, QueueApiV1Routes}
import uk.gov.homeoffice.drt.services.api.v1.{FlightExport, QueueExport}
import uk.gov.homeoffice.drt.services.s3.S3Service
import uk.gov.homeoffice.drt.services.{PassengerSummaryStreams, UserRequestService, UserService}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, UtcDate}
import uk.gov.homeoffice.drt.uploadTraining.FeatureGuideService

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class KeyCloakConfig(url: String,
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
                        healthCheckFrequencyMinutes: Int,
                        enabledPorts: Seq[PortCode],
                        slackUrl: String
                       ) {
  val clientConfig: ClientConfig = ClientConfig(portRegions, portTerminals, rootDomain, teamEmail)
  val keyClockConfig: KeyCloakConfig = KeyCloakConfig(keycloakUrl, keycloakTokenUrl, keycloakClientId, keycloakClientSecret)
}

object Server {
  private val log = LoggerFactory.getLogger(getClass)

  sealed trait Message

  private final case class StartFailed(cause: Throwable) extends Message

  private final case class Started(binding: ServerBinding) extends Message

  private case object Stop extends Message

  val healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] = Seq(
    ApiHealthCheck(hoursBeforeNow = 2, hoursAfterNow = 1, minimumFlights = 4, passThresholdPercentage = 50, SDate.now),
    ArrivalLandingTimesHealthCheck(windowLength = 2.hours, buffer = 20, minimumFlights = 3, passThresholdPercentage = 50, SDate.now),
  )

  private val nonMlPaxPorts = Set("ABZ", "EXT", "HUY", "INV", "LHR", "MME", "NQY", "NWI", "PIK", "SEN")

  val paxFeedSourceOrder: PortCode => List[FeedSource] =
    portCode => if (!nonMlPaxPorts.contains(portCode.iata)) List(
      ScenarioSimulationSource,
      LiveFeedSource,
      ApiFeedSource,
      MlFeedSource,
      ForecastFeedSource,
      HistoricApiFeedSource,
      AclFeedSource,
    ) else List(
      ScenarioSimulationSource,
      LiveFeedSource,
      ApiFeedSource,
      ForecastFeedSource,
      HistoricApiFeedSource,
      AclFeedSource,
    )

  def apply(config: ServerConfig,
            notifications: EmailNotifications,
            emailClient: EmailClient,
           ): Behavior[Message] =
    Behaviors.setup { ctx: ActorContext[Message] =>
      implicit val system: ActorSystem[Nothing] = ctx.system
      val systemClassic = system.classicSystem
      implicit val mat: Materializer = Materializer.matFromSystem(systemClassic)
      implicit val ec: ExecutionContextExecutor = system.executionContext
      implicit val timeout: Timeout = new Timeout(1.second)

      val now = () => SDate.now()

      val defaultQueueSlotMinutes = 15
      val urls = Urls(config.rootDomain, config.useHttps)
      val userRequestService = UserRequestService(UserAccessRequestDao(ProdDatabase.db))
      val userService = UserService(UserDao(ProdDatabase.db))
      val dropInDao = DropInDao(ProdDatabase.db)
      val dropInRegistrationDao = DropInRegistrationDao(ProdDatabase.db)
      val userFeedbackDao = UserFeedbackDao(ProdDatabase.db)
      val featureGuideService = FeatureGuideService(FeatureGuideDao(ProdDatabase.db), FeatureGuideViewDao(ProdDatabase.db))

      val (exportUploader, exportDownloader) = S3Service.s3FileUploaderAndDownloader(config, config.exportsFolderPrefix)
      val (featureUploader, featureDownloader) = S3Service.s3FileUploaderAndDownloader(config, config.featureFolderPrefix)

      implicit val db: AppDatabase = ProdDatabase

      val indexRoutes = IndexRoute(urls, indexResource = getFromResource("frontend/index.html")).route

      val sendHttpRequest = (request: HttpRequest) => Http()(mat.system).singleRequest(request)
      val httpClient = ProdHttpClient(sendHttpRequest)

      val slackClient =
        if (config.slackUrl.nonEmpty) SlackClientImpl(httpClient, config.slackUrl)
        else NoopSlackClient

      val healthChecksActor = startHealthChecksActor(slackClient, urls)
      val getAlarmStatuses: () => Future[Map[PortCode, Map[String, Boolean]]] = () => healthChecksActor.ask(replyTo => HealthChecksActor.GetAlarmStatuses(replyTo))

      val keyCloakAuth = KeyCloakAuth(config.keycloakTokenUrl, config.keycloakClientId, config.keycloakClientSecret, sendHttpRequest)

      val queuesForPortAndDatesAndSlotSize: (PortCode, Terminal, LocalDate, LocalDate) => Source[(UtcDate, Seq[CrunchMinute]), NotUsed] = {
        (port, terminal, start, end) =>
          QueueSlotDao().queueSlotsForDateRange(port, defaultQueueSlotMinutes, db.run)(start, end, Seq(terminal))
      }

      val routes: Route = concat(
        pathPrefix("api") {
          concat(
            pathPrefix("v1") {
              concat(
                QueueApiV1Routes(config.enabledPorts, QueueExport.queues(queuesForPortAndDatesAndSlotSize)),
                FlightApiV1Routes(config.enabledPorts, FlightExport.flights(db)),
                AuthApiV1Routes(keyCloakAuth.getToken),
              )
            },
            PassengerRoutes(PassengerSummaryStreams(db).streamForGranularity),
            CiriumRoutes(config.ciriumDataUri),
            ConfigRoutes(config.clientConfig),
            LegacyExportRoutes(httpClient, exportUploader.upload, exportDownloader.download, now),
            ExportRoutes(httpClient, exportUploader.upload, exportDownloader.download, ExportPersistenceImpl(db), now, emailClient, urls.rootUrl, config.teamEmail),
            UserRoutes(config.clientConfig, userService, userRequestService, notifications, config.keycloakUrl),
            FeatureGuideRoutes(featureGuideService, featureUploader, featureDownloader),
            AlertsRoutes(),
            HealthCheckRoutes(getAlarmStatuses, healthChecks, ScheduledHealthCheckPausePersistenceImpl(db, now)),
            DropInSessionsRoute(dropInDao),
            DropInRegisterRoutes(dropInRegistrationDao),
            FeedbackRoutes(userFeedbackDao),
            ExportConfigRoutes(httpClient, config.enabledPorts),
          )
        },
        indexRoutes,
      )

      val serverBinding = Http().newServerAt(config.host, config.port).bind(routes)

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

            val monitor: Cancellable = startHealthCheckMonitor(config, db, healthChecksActor)

            running(binding, monitor)

          case Stop =>
            // we got a stop message but haven't completed starting yet,
            // we cannot stop until starting has completed
            starting(wasStopped = true)
        }

      starting(wasStopped = false)
    }

  private def startHealthChecksActor(slackClient: SlackClient,
                                     urls: Urls,
                                    )
                                    (implicit system: ActorSystem[Nothing], ec: ExecutionContext): ActorRef[HealthChecksActor.Command] = {
    def sendSlackNotification(portCode: PortCode, checkName: String, priority: IncidentPriority, status: String): Unit = {
      val port = portCode.toString.toUpperCase
      val link = urls.urlForPort(port)
      val message = s"$port ${priority.name} $status $checkName - $link"
      slackClient.notify(message)
    }

    val soundAlarm = (portCode: PortCode, checkName: String, priority: IncidentPriority) => {
      log.info(s"Sound alarm for $portCode $checkName $priority")
      sendSlackNotification(portCode, checkName, priority, "triggered")
    }

    val silenceAlarm = (portCode: PortCode, checkName: String, priority: IncidentPriority) => {
      log.info(s"Silence alarm for $portCode $checkName $priority")
      sendSlackNotification(portCode, checkName, priority, "resolved")
    }

    val alarmTriggerConsecutiveFailures = 3
    val retainMaxResponses = 5

    val behaviour = HealthChecksActor(soundAlarm, silenceAlarm, () => SDate.now().millisSinceEpoch, alarmTriggerConsecutiveFailures, retainMaxResponses, Map.empty)
    system.systemActorOf(behaviour, "health-checks")
  }

  private def startHealthCheckMonitor(serverConfig: ServerConfig,
                                      db: AppDatabase,
                                      healthChecksActor: ActorRef[HealthChecksActor.Command],
                                     )
                                     (implicit
                                      system: ActorSystem[Nothing],
                                      ec: ExecutionContext,
                                      mat: Materializer,
                                     ): Cancellable = {
    implicit val timeout: Timeout = new Timeout(1.second)

    val poolSettings = ConnectionPoolSettings(system)
      .withMaxConnectionBackoff(5.seconds)
      .withBaseConnectionBackoff(1.second)
      .withMaxRetries(0)
      .withMaxConnections(5)
    val makeRequest = (request: HttpRequest) => Http().singleRequest(request, settings = poolSettings)
    val recordResponse = (port: PortCode, response: HealthCheckResponse[_]) =>
      healthChecksActor.ask(replyTo => HealthChecksActor.PortHealthCheckResponse(port, response, replyTo))

    log.info(s"Starting health check monitor for ports ${serverConfig.portTerminals.keys.mkString(", ")}")
    val monitor = HealthCheckMonitor(makeRequest, recordResponse, serverConfig.portTerminals.keys, healthChecks)
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
    system.scheduler.scheduleWithFixedDelay(30.seconds, serverConfig.healthCheckFrequencyMinutes.minutes)(Check)
  }
}
