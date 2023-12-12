package uk.gov.homeoffice.drt.routes

import akka.Done
import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.StatusCodes.{InternalServerError, NotFound}
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
import uk.gov.homeoffice.drt.exports.{ExportPort, ExportType, PortExportType}
import uk.gov.homeoffice.drt.json.ExportJsonFormats.exportRequestJsonFormat
import uk.gov.homeoffice.drt.models.Export
import uk.gov.homeoffice.drt.notifications.EmailClient
import uk.gov.homeoffice.drt.notifications.templates.DownloadManagerTemplates
import uk.gov.homeoffice.drt.persistence.ExportPersistence
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.rccu.ExportCsvService
import uk.gov.homeoffice.drt.rccu.ExportCsvService.getUri
import uk.gov.homeoffice.drt.time.{LocalDate, SDateLike}

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
           )
           (implicit ec: ExecutionContext, mat: Materializer): Route = {
    lazy val exportCsvService = ExportCsvService(httpClient)
    headerValueByName("X-Auth-Email") { email =>
      pathPrefix("export")(
        concat(
          pathEnd(
            post(entity(as[ExportRequest]) { exportRequest =>
              handleExport(upload, exportPersistence, exportCsvService, email, exportRequest, now, emailClient, rootUrl, teamEmail)
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
                  case Success(Some(export)) => complete(s"""{"status": "${export.status}"}""")
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
      )
    }
  }

  private def getExportRoute(email: String,
                             createdAt: String,
                             exportCsvService: ExportCsvService,
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
                           exportCsvService: ExportCsvService,
                           email: String,
                           exportRequest: ExportRequest,
                           now: () => SDateLike,
                           emailClient: EmailClient,
                           rootDomain: String,
                           teamEmail: String,
                          )
                          (implicit ec: ExecutionContext, mat: Materializer): StandardRoute = {
    val startDateString = exportRequest.startDate.toString()
    val endDateString = exportRequest.endDate.toString()
    val creationDate = now()
    val fileName = exportCsvService.makeFileName(startDateString, endDateString, creationDate)

    val export = Export(email, exportRequest.ports.map(ep => ep.terminals.map(t => s"${ep.port}-$t").mkString("_")).mkString("__"), exportRequest.startDate, exportRequest.endDate, "preparing", creationDate)
    exportPersistence.insert(export)
      .map(_ => log.info("Export inserted"))
      .recover { case e => log.error("Failed to insert export", e) }

    val stream = Source(exportRequest.ports.toList.sortBy(_.port))
      .mapConcat { exportPort =>
        val portCode = PortCode(exportPort.port)
        AirportConfigs.confByPort.get(portCode).map(config => (exportPort, config.terminals))
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
            .getPortResponseForTerminal(uri, portCode)
            .recover { e =>
              log.error(s"Failed to get response from $uri", e)

              handleReportFailure(emailClient, export, teamEmail, exportPersistence)

              throw new Exception("Failed to get port response")
            }
      }
      .prepend(Source.single(ByteString(exportRequest.exportType.headerRow + "\n")))

    upload(fileName, stream).onComplete {
      case Success(_) =>
        handleReportReady(emailClient, rootDomain, export, exportPersistence)
        log.info(s"Export complete: $fileName")
      case Failure(exception) =>
        handleReportFailure(emailClient, export, teamEmail, exportPersistence)
        log.error("Failed to create export", exception)
    }
    complete(s"""{"status": "${export.status}", "createdAt": ${export.createdAt.millisSinceEpoch}}""")
  }

  private def handleReportReady(emailClient: EmailClient,
                                rootDomain: String,
                                export: Export,
                                exportPersistence: ExportPersistence,
                               ): Unit = {
    exportPersistence.update(export.copy(status = "complete"))
    val link = s"$rootDomain/api/export/${export.createdAt.millisSinceEpoch}"
    val emailSuccess = emailClient.send(DownloadManagerTemplates.reportReadyTemplateId, export.email, Map("download_link" -> link))

    if (!emailSuccess) log.error("Failed to send email")
  }

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
