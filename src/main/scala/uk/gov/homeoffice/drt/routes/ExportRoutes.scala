package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.headers.ContentDispositionTypes.attachment
import akka.http.scaladsl.model.headers.`Content-Disposition`
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Route, StandardRoute, ValidationRejection}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.arrivals.ArrivalExportHeadings
import uk.gov.homeoffice.drt.db.ProdDatabase.db
import uk.gov.homeoffice.drt.db.RegionExportQueries
import uk.gov.homeoffice.drt.json.RegionExportJsonFormats._
import uk.gov.homeoffice.drt.models.RegionExport
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.rccu.ExportCsvService
import uk.gov.homeoffice.drt.services.s3.{S3Downloader, S3Uploader}
import uk.gov.homeoffice.drt.time.{LocalDate, SDate}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}


object ExportRoutes {
  private val log = LoggerFactory.getLogger(getClass)

  case class RegionExportRequest(region: String, startDate: LocalDate, endDate: LocalDate)

  implicit val csvStreaming: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withFramingRenderer(Flow[ByteString])
  implicit val csvMarshaller: ToEntityMarshaller[ByteString] =
    Marshaller.withFixedContentType(ContentTypes.`text/csv(UTF-8)`) { bytes =>
      HttpEntity(ContentTypes.`text/csv(UTF-8)`, bytes)
    }

  def apply(httpClient: HttpClient, s3Uploader: S3Uploader, s3Downloader: S3Downloader)
           (implicit ec: ExecutionContextExecutor, mat: Materializer): Route = {
    lazy val exportCsvService = new ExportCsvService(httpClient)
    headerValueByName("X-Auth-Email") { email =>
      pathPrefix("export")(
        concat(
          post(
            entity(as[RegionExportRequest]) { exportRequest =>
              handleRegionExport(s3Uploader, exportCsvService, email, exportRequest)
            }
          ),
          get {
            concat(
              path(Segment) { region =>
                complete(db.run(RegionExportQueries.getAll(email, region)))
              },
              path(Segment / Segment) { case (region, createdAt) =>
                log.info(s"Getting region export for $email / $region / $createdAt")
                val eventualStream = db.run(RegionExportQueries.get(email, region, createdAt.toLong))
                  .flatMap {
                    case Some(regionExport) =>
                      val startDateString = regionExport.startDate.toString()
                      val endDateString = regionExport.endDate.toString()
                      val fileName = s"exports/${exportCsvService.makeFileName(startDateString, endDateString, regionExport.region, regionExport.createdAt)}"
                      log.info(s"Downloading $fileName")
                      s3Downloader.download(fileName).map { stream =>
                        respondWithHeader(`Content-Disposition`(attachment, Map("filename" -> fileName))) {
                          complete(stream)
                        }
                      }
                    case None =>
                      Future.successful(complete("Region export not found"))
                  }
                onComplete(eventualStream) {
                  case Success(route) => route
                  case Failure(e) =>
                    log.error("Failed to get region export", e)
                    complete("Failed to get region export")
                }
              }
            )
          }
        )
      )
    }
  }

  private def handleRegionExport(s3Uploader: S3Uploader,
                                 exportCsvService: => ExportCsvService,
                                 email: String,
                                 exportRequest: RegionExportRequest)
                                (implicit ec: ExecutionContextExecutor, mat: Materializer): StandardRoute = {
    val startDateString = exportRequest.startDate.toString()
    val endDateString = exportRequest.endDate.toString()
    val fileName = exportCsvService.makeFileName(startDateString, endDateString, exportRequest.region, SDate.now())
    exportCsvService.getPortRegion(exportRequest.region).map { portRegion: PortRegion =>
      val regionExport = RegionExport(email, portRegion.name, exportRequest.startDate, exportRequest.endDate, "preparing", SDate.now())
      db.run(RegionExportQueries.insert(regionExport))
        .map(_ => log.info("Region export inserted"))
        .recover { case e => log.error("Failed to insert region export", e) }

      val stream = Source(portRegion.ports.toList.sortBy(_.iata))
        .map { port =>
          AirportConfigs.confByPort.get(port).map(config => (port.iata, config.terminals))
        }
        .mapConcat {
          case Some((portStr, terminals)) => terminals.map(t => (portStr, t))
        }
        .mapAsync(1) {
          case (port, terminal) =>
            exportCsvService
              .getPortResponseForTerminal(startDateString, endDateString, portRegion.name, port, terminal.toString)
              .recover { e =>
                log.error(s"Failed to get port response for $port $terminal", e)

                updateExportStatus(regionExport, "failed")

                throw new Exception("Failed to get port response")
              }
        }
        .prepend(Source.single(ByteString(ArrivalExportHeadings.regionalExportHeadings)))

      s3Uploader.upload(fileName, stream).onComplete {
        case Success(_) =>
          updateExportStatus(regionExport, "complete")
          log.info("Export complete")
        case Failure(exception) =>
          updateExportStatus(regionExport, "failed")
          log.error("Failed to create export", exception)
      }
      complete("ok")
    }.getOrElse(reject(ValidationRejection("Region not found.")))
  }

  private def updateExportStatus(regionExport: RegionExport, status: String)
                                (implicit ec: ExecutionContext): Future[Boolean] = {
    val updatedRegionExport = regionExport.copy(status = status)
    db.run(RegionExportQueries.update(updatedRegionExport))
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
