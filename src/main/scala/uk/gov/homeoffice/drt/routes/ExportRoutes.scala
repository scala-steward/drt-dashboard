package uk.gov.homeoffice.drt.routes

import org.apache.pekko.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.pekko.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import org.apache.pekko.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
import org.apache.pekko.http.scaladsl.model.headers.ContentDispositionTypes.attachment
import org.apache.pekko.http.scaladsl.model.headers.`Content-Disposition`
import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{Route, StandardRoute}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{Flow, Sink, Source}
import org.apache.pekko.util.ByteString
import org.apache.pekko.{Done, NotUsed}
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.arrivals.ApiFlightWithSplits
import uk.gov.homeoffice.drt.exports.{Arrivals, ExportPort, ExportType, PortExportType}
import uk.gov.homeoffice.drt.json.ExportJsonFormats.exportRequestJsonFormat
import uk.gov.homeoffice.drt.models.{Export, UniqueArrivalKey, VoyageManifest, VoyageManifests}
import uk.gov.homeoffice.drt.notifications.EmailClient
import uk.gov.homeoffice.drt.notifications.templates.DownloadManagerTemplates
import uk.gov.homeoffice.drt.persistence.ExportPersistence
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.rccu.RestExportCsvService
import uk.gov.homeoffice.drt.rccu.RestExportCsvService.getUri
import uk.gov.homeoffice.drt.services.exports.{FlightsWithSplitsExport, FlightsWithSplitsMultiRegionExportImpl}
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike, UtcDate}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object ExportRoutes {
  private val log = LoggerFactory.getLogger(getClass)

  case class ExportRequest(exportType: ExportType, ports: Seq[ExportPort], startDate: LocalDate, endDate: LocalDate)

  implicit val csvStreaming: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withFramingRenderer(Flow[ByteString])
  implicit val csvMarshaller: ToEntityMarshaller[ByteString] =
    Marshaller.withFixedContentType(ContentTypes.`text/csv(UTF-8)`) { bytes =>
      HttpEntity(ContentTypes.`text/csv(UTF-8)`, bytes)
    }

  def apply(httpClient: HttpClient,
            upload: (String, Source[ByteString, Any]) => Future[Done],
            download: String => Future[Source[ByteString, _]],
            exportPersistence: ExportPersistence,
            now: () => SDateLike,
            emailClient: EmailClient,
            rootUrl: String,
            teamEmail: String,
            manifestProvider: UniqueArrivalKey => Future[Option[VoyageManifest]],
            flightsProvider: (PortCode, LocalDate, LocalDate) => Source[(UtcDate, Iterable[ApiFlightWithSplits]), NotUsed],
            feedSourceOrder: PortCode => List[FeedSource],
           )
           (implicit ec: ExecutionContext, mat: Materializer): Route = {
    lazy val exportCsvService = RestExportCsvService(httpClient)
    pathPrefix("export") {
      headerValueByName("X-Forwarded-Email") { email =>
        concat(
          pathEnd(
            post(entity(as[ExportRequest]) { exportRequest =>
              handleExport(upload, exportPersistence, exportCsvService, email, exportRequest, now, emailClient, rootUrl, teamEmail, rootUrl, manifestProvider, flightsProvider, feedSourceOrder)
            })
          ),
          get {
            concat(
              pathEnd {
                import uk.gov.homeoffice.drt.json.ExportJsonFormats._
                complete(exportPersistence.getAll(email))
              },
              path("status" / Segment) { createdAt =>
                onComplete(exportPersistence.get(email, createdAt.toLong)) {
                  case Success(Some(export)) =>
                    complete(s"""{"status": "${export.status}"}""")
                  case Success(None) => complete(NotFound)
                  case Failure(e) =>
                    log.error("Failed to get export", e)
                    complete(InternalServerError)
                }
              },
              path(Segment) { createdAt =>
                onComplete(getExportRoute(email, createdAt, exportCsvService, download, exportPersistence)) {
                  case Success(route) => route
                  case Failure(e) =>
                    log.error("Failed to get export", e)
                    complete("Failed to get export")
                }
              }
            )
          }
        )
      }
    }
  }

  private def getExportRoute(email: String,
                             createdAt: String,
                             exportCsvService: RestExportCsvService,
                             downloader: String => Future[Source[ByteString, _]],
                             exportPersistence: ExportPersistence,
                            )
                            (implicit ec: ExecutionContext): Future[Route] = {
    log.info(s"Getting export for $email/ $createdAt")

    exportPersistence.get(email, createdAt.toLong)
      .flatMap {
        case Some(export) =>
          val startDateString = export.startDate.toString()
          val endDateString = export.endDate.toString()
          val fileName = exportCsvService.makeFileName(startDateString, endDateString, export.createdAt)
          log.info(s"Downloading $fileName")
          downloader(fileName).map { stream =>
            respondWithHeader(`Content-Disposition`(attachment, Map("filename" -> fileName))) {
              complete(stream)
            }
          }
        case None =>
          Future.successful(complete("Export not found"))
      }
  }

  private def handleExport(upload: (String, Source[ByteString, Any]) => Future[Done],
                           exportPersistence: ExportPersistence,
                           restExportCsvService: RestExportCsvService,
                           email: String,
                           exportRequest: ExportRequest,
                           now: () => SDateLike,
                           emailClient: EmailClient,
                           rootDomain: String,
                           teamEmail: String,
                           rootUrl: String,
                           manifestProvider: UniqueArrivalKey => Future[Option[VoyageManifest]],
                           flightsProvider: (PortCode, LocalDate, LocalDate) => Source[(UtcDate, Iterable[ApiFlightWithSplits]), NotUsed],
                           feedSourceOrder: PortCode => List[FeedSource],
                          )
                          (implicit ec: ExecutionContext, mat: Materializer): StandardRoute = {
    val startDateString = exportRequest.startDate.toString()
    val endDateString = exportRequest.endDate.toString()
    val creationDate = now()
    val fileName = restExportCsvService.makeFileName(startDateString, endDateString, creationDate)

    val export = Export(email, exportRequest.ports.map(ep => ep.terminals.map(t => s"${ep.port}-$t").mkString("_")).mkString("__"), exportRequest.startDate, exportRequest.endDate, "preparing", creationDate)
    exportPersistence.insert(export)
      .map(_ => log.info("Export inserted"))
      .recover { case e => log.error("Failed to insert export", e) }

    val stream = exportRequest.exportType match {
      case Arrivals =>
        Source(exportRequest.ports.toList.sortBy(_.port))
          .flatMap { exportPort =>
            val portCode = PortCode(exportPort.port)
            val portSourceOrder = feedSourceOrder(portCode)
            val terminals = AirportConfigs.confByPort.get(portCode).map(_.terminals).getOrElse(Seq.empty).toSeq
            val fwsExport = FlightsWithSplitsMultiRegionExportImpl(exportRequest.startDate, exportRequest.endDate, portCode, terminals, portSourceOrder)
            requestToCsvStream(fwsExport, portCode, manifestProvider, flightsProvider)
          }
      case _ =>
        restExportStream(exportRequest, restExportCsvService)
    }
    val streamWithHeader = stream.prepend(Source.single(ByteString(exportRequest.exportType.headerRow + "\n")))

    upload(fileName, streamWithHeader).onComplete {
      case Success(_) =>
        handleReportReady(emailClient, rootDomain, export, exportPersistence)
        log.info(s"Export complete: $fileName")
      case Failure(exception) =>
        handleReportFailure(emailClient, export, teamEmail, exportPersistence)
        log.error("Failed to create export", exception)
    }
    complete(s"""{"status": "${export.status}", "createdAt": ${export.createdAt.millisSinceEpoch}, "downloadLink": "${downloadUrl(rootUrl, export)}"}""")
  }

  private def restExportStream(exportRequest: ExportRequest, exportCsvService: RestExportCsvService)
                              (implicit ec: ExecutionContext, mat: Materializer): Source[ByteString, NotUsed] = {
    Source(exportRequest.ports.toList.sortBy(_.port))
      .mapConcat { exportPort =>
        val portCode = PortCode(exportPort.port)

        exportRequest.exportType match {
          case _: PortExportType =>
            val uri = getUri(exportRequest.exportType, exportRequest.startDate, exportRequest.endDate, portCode, None)
            Seq((uri, portCode))
          case _ =>
            exportPort.terminals.map { t =>
              val uri = getUri(exportRequest.exportType, exportRequest.startDate, exportRequest.endDate, portCode, Option(Terminal(t)))
              (uri, portCode)
            }
        }
      }
      .mapAsync(1) {
        case (uri, portCode) =>
          exportCsvService
            .responseContentAsByteString(uri, portCode)
            .recover { e =>
              log.error(s"Failed to get response from $uri", e)
              throw new Exception("Failed to get port response", e)
            }
      }
  }

  private def requestToCsvStream(`export`: FlightsWithSplitsExport,
                                 portCode: PortCode,
                                 manifestProvider: UniqueArrivalKey => Future[Option[VoyageManifest]],
                                 flightsProvider: (PortCode, LocalDate, LocalDate) => Source[(UtcDate, Iterable[ApiFlightWithSplits]), NotUsed],
                                )
                                (implicit ec: ExecutionContext, mat: Materializer): Source[ByteString, NotUsed] =
    export
      .csvStream(flightsProvider(portCode, `export`.start, `export`.end).mapAsync(1) { case (d, flights) =>
        val sortedFlights = flights.toSeq.sortBy(_.apiFlight.PcpTime.getOrElse(0L))
        addLiveManifestsForFlights(portCode, sortedFlights, manifestProvider)
      })
      .map(s => ByteString(s, "UTF-8"))


  private def addLiveManifestsForFlights(portCode: PortCode,
                                         flights: Seq[ApiFlightWithSplits],
                                         manifestProvider: UniqueArrivalKey => Future[Option[VoyageManifest]],
                                        )
                                        (implicit ec: ExecutionContext, mat: Materializer): Future[(Seq[ApiFlightWithSplits], VoyageManifests)] =
    Source(flights)
      .mapAsync(1) { fws =>
        manifestProvider(UniqueArrivalKey(fws.apiFlight, portCode))
      }
      .collect {
        case Some(vm) => vm
      }
      .runWith(Sink.seq)
      .map(m => (flights, VoyageManifests(m)))

  private def handleReportReady(emailClient: EmailClient,
                                rootDomain: String,
                                export: Export,
                                exportPersistence: ExportPersistence,
                               ): Unit = {
    exportPersistence.update(export.copy(status = "complete"))
    val link = downloadUrl(rootDomain, export)
    val emailSuccess = emailClient.send(DownloadManagerTemplates.reportReadyTemplateId, export.email, Map("download_link" -> link))

    if (!emailSuccess) log.error("Failed to send email")
  }

  private def downloadUrl(rootDomain: String, export: Export): String = s"$rootDomain/api/export/${export.createdAt.millisSinceEpoch}"

  private def handleReportFailure(emailClient: EmailClient,
                                  export: Export,
                                  teamEmail: String,
                                  exportPersistence: ExportPersistence,
                                 ): Unit = {
    exportPersistence.update(export.copy(status = "failed"))
    val emailSuccess = emailClient.send(DownloadManagerTemplates.reportFailedTemplateId, export.email, Map("support_email" -> teamEmail))

    if (!emailSuccess) log.error("Failed to send email")
  }
}
