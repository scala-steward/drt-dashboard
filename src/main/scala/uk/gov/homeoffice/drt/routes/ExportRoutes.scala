package uk.gov.homeoffice.drt.routes

import akka.Done
import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.headers.ContentDispositionTypes.attachment
import akka.http.scaladsl.model.headers.`Content-Disposition`
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.db.{AppDatabase, ExportQueries}
import uk.gov.homeoffice.drt.exports.{ExportPort, ExportType}
import uk.gov.homeoffice.drt.json.ExportJsonFormats.exportRequestJsonFormat
import uk.gov.homeoffice.drt.models.Export
import uk.gov.homeoffice.drt.notifications.EmailClient
import uk.gov.homeoffice.drt.notifications.templates.DownloadManagerTemplates
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.rccu.ExportCsvService
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
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
            now: () => SDateLike,
            emailClient: EmailClient,
            rootUrl: String,
            teamEmail: String,
           )
           (implicit ec: ExecutionContextExecutor, mat: Materializer, database: AppDatabase): Route = {
    lazy val exportCsvService = ExportCsvService(httpClient)
    headerValueByName("X-Auth-Email") { email =>
      pathPrefix("export")(
        concat(
          post(
            entity(as[ExportRequest]) { exportRequest =>
              handleExport(upload, exportCsvService, email, exportRequest, now, emailClient, rootUrl, teamEmail)
            }
          ),
          get {
            concat(
              pathEnd {
                import uk.gov.homeoffice.drt.json.ExportJsonFormats._
                complete(database.db.run(ExportQueries.getAll(email)))
              },
              path(Segment) { createdAt =>
                onComplete(getExportRoute(email, createdAt, exportCsvService, download)) {
                  case Success(route) => route
                  case Failure(e) =>
                    log.error("Failed to get export", e)
                    complete("Failed to get export")
                }
              }
            )
          }
        )
      )
    }
  }

  private def getExportRoute(email: String,
                             createdAt: String,
                             exportCsvService: ExportCsvService,
                             downloader: String => Future[Source[ByteString, _]],
                            )
                            (implicit ec: ExecutionContextExecutor, database: AppDatabase): Future[Route] = {
    log.info(s"Getting region export for $email/ $createdAt")

    database.db.run(ExportQueries.get(email, createdAt.toLong))
      .flatMap {
        case Some(regionExport) =>
          val startDateString = regionExport.startDate.toString()
          val endDateString = regionExport.endDate.toString()
          val fileName = exportCsvService.makeFileName(startDateString, endDateString, regionExport.createdAt)
          log.info(s"Downloading $fileName")
          downloader(fileName).map { stream =>
            respondWithHeader(`Content-Disposition`(attachment, Map("filename" -> fileName))) {
              complete(stream)
            }
          }
        case None =>
          Future.successful(complete("Region export not found"))
      }
  }

  private def handleExport(upload: (String, Source[ByteString, Any]) => Future[Done],
                           exportCsvService: => ExportCsvService,
                           email: String,
                           exportRequest: ExportRequest,
                           now: () => SDateLike,
                           emailClient: EmailClient,
                           rootDomain: String,
                           teamEmail: String,
                          )
                          (implicit ec: ExecutionContextExecutor, mat: Materializer, database: AppDatabase): StandardRoute = {
    val startDateString = exportRequest.startDate.toString()
    val endDateString = exportRequest.endDate.toString()
    val creationDate = now()
    val fileName = exportCsvService.makeFileName(startDateString, endDateString, creationDate)

    val regionExport = Export(email, exportRequest.ports.map(ep => ep.terminals.map(t => s"${ep.port}-$t").mkString("_")).mkString("__"), exportRequest.startDate, exportRequest.endDate, "preparing", creationDate)
    database.db.run(ExportQueries.insert(regionExport))
      .map(_ => log.info("Region export inserted"))
      .recover { case e => log.error("Failed to insert region export", e) }

    val stream = Source(exportRequest.ports.toList.sortBy(_.port))
      .mapConcat { exportPort =>
        AirportConfigs.confByPort.get(PortCode(exportPort.port)).map(config => (exportPort, config.terminals))
        exportPort.terminals.map(t => (PortCode(exportPort.port), Terminal(t)))
      }
      .mapAsync(1) {
        case (port, terminal) =>
          exportCsvService
            .getPortResponseForTerminal(exportRequest.exportType, exportRequest.startDate, exportRequest.endDate, port, terminal)
            .recover { e =>
              log.error(s"Failed to get port response for $port $terminal", e)

              handleReportFailure(emailClient, regionExport, teamEmail)

              throw new Exception("Failed to get port response")
            }
      }
      .prepend(Source.single(ByteString(exportRequest.exportType.headerRow + "\n")))

    upload(fileName, stream).onComplete {
      case Success(_) =>
        handleReportReady(emailClient, rootDomain, regionExport)
        log.info(s"Export complete: $fileName")
      case Failure(exception) =>
        handleReportFailure(emailClient, regionExport, teamEmail)
        log.error("Failed to create export", exception)
    }
    complete("ok")
  }

  private def handleReportReady(emailClient: EmailClient, rootDomain: String, regionExport: Export)
                               (implicit ec: ExecutionContextExecutor, database: AppDatabase): Unit = {
    updateExportStatus(regionExport, "complete")
    val link = s"$rootDomain/export/${regionExport.createdAt.millisSinceEpoch}"
    val emailSuccess = emailClient.send(DownloadManagerTemplates.reportReadyTemplateId, regionExport.email, Map("download_link" -> link))

    if (!emailSuccess) log.error("Failed to send email")
  }

  private def handleReportFailure(emailClient: EmailClient, regionExport: Export, teamEmail: String)
                                 (implicit ec: ExecutionContextExecutor, database: AppDatabase): Unit = {
    updateExportStatus(regionExport, "failed")
    val emailSuccess = emailClient.send(DownloadManagerTemplates.reportFailedTemplateId, regionExport.email, Map("support_email" -> teamEmail))

    if (!emailSuccess) log.error("Failed to send email")
  }

  private def updateExportStatus(`export`: Export, status: String)
                                (implicit ec: ExecutionContext, database: AppDatabase): Future[Boolean] = {
    val updatedExport = `export`.copy(status = status)
    database.db.run(ExportQueries.update(updatedExport))
      .map { _ =>
        log.info("Region export updated")
        true
      }
      .recover {
        case e =>
          log.error("Failed to update region export", e)
          false
      }
  }
}
