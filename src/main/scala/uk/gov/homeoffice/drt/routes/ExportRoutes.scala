package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.common.{ CsvEntityStreamingSupport, EntityStreamingSupport }
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.headers.ContentDispositionTypes.attachment
import akka.http.scaladsl.model.headers.`Content-Disposition`
import akka.http.scaladsl.server.Directives.{ complete, _ }
import akka.http.scaladsl.server.{ Route, ValidationRejection }
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.arrivals.ArrivalsHeadings.rccHeadings
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.rccu.ExportCsvService

import scala.concurrent.ExecutionContextExecutor

object ExportRoutes {

  def apply(httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Route = {
    lazy val exportCsvService = new ExportCsvService(httpClient)
    path("export" / Segment / Segment / Segment) { (region, startDate, endDate) =>
      val fileName = exportCsvService.makeFileName(startDate, endDate, region)
      exportCsvService.getPortRegion(region).map { portRegion: PortRegion =>
        respondWithHeader(`Content-Disposition`(attachment, Map("filename" -> fileName))) {
          complete(Source(portRegion.ports.toList)
            .map { port =>
              AirportConfigs.confByPort.get(port).map(config => (port.iata, config.terminals))
            }
            .mapConcat {
              case Some((portStr, terminals)) => terminals.map(t => (portStr, t))
            }.flatMapConcat {
              case (port, terminal) =>
                Source.future(exportCsvService.getPortResponseForTerminal(startDate, endDate, portRegion.name, port, terminal.toString))
                  .flatMapConcat { portResponse =>
                    portResponse.map(pr => pr.httpResponse.entity.dataBytes
                      .map {
                        _
                          .utf8String
                          .split("\n")
                          .filterNot(_.contains("ICAO"))
                          .map(line => s"${region.name},${pr.port},${pr.terminal},$line")
                          .mkString("\n")
                      }).getOrElse(Source.empty)
                      .filterNot(_.isEmpty)
                  }
            }.prepend(Source.single(rccHeadings)))
        }
      }.getOrElse(reject(ValidationRejection("Region not found.")))
    }

  }

  val contentType = ContentTypes.`text/plain(UTF-8)`

  implicit val streamingSupport: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withContentType(contentType)

}
