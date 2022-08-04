package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.common.{ CsvEntityStreamingSupport, EntityStreamingSupport }
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.server.Directives.{ complete, _ }
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.ports.PortRegion
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.rccu.ExportCsvService
import scala.concurrent.ExecutionContextExecutor

object ExportRoutes {

  def apply(httpClient: HttpClient)(implicit ec: ExecutionContextExecutor, mat: Materializer): Route = {
    lazy val exportCsvService = new ExportCsvService(httpClient)
    path("export" / Segment / Segment / Segment) { (region, startDate, endDate) =>
      val portRegion: PortRegion = exportCsvService.getPortRegion(region)
      val portCsvHeadings = "IATA,ICAO,Origin,Gate/Stand,Status,Scheduled,Est Arrival,Act Arrival,Est Chox,Act Chox,Minutes off scheduled,Est PCP,Total Pax,PCP Pax,Invalid API,API e-Gates,API EEA,API Non-EEA,API Fast Track,Historical e-Gates,Historical EEA,Historical Non-EEA,Historical Fast Track,Terminal Average e-Gates,Terminal Average EEA,Terminal Average Non-EEA,Terminal Average Fast Track,API Actual - EEA Machine Readable to e-Gates,API Actual - EEA Machine Readable to EEA,API Actual - EEA Non-Machine Readable to EEA,API Actual - EEA Child to EEA,API Actual - GBR National to e-Gates,API Actual - GBR National to EEA,API Actual - GBR National Child to EEA,API Actual - B5J+ National to e-Gates,API Actual - B5J+ National to EEA,API Actual - B5J+ Child to EEA,API Actual - Visa National to Non-EEA,API Actual - Non-Visa National to Non-EEA,API Actual - Visa National to Fast Track,API Actual - Non-Visa National to Fast Track,Nationalities,Ages"
      val headings = "Region,Port,Terminal," + portCsvHeadings
      complete(
        Source(portRegion.ports.toList)
          .map { port =>
            AirportConfigs.confByPort.get(port).map(config => (port.iata, config.terminals))
          }
          .mapConcat {
            case Some((portStr, terminals)) => terminals.map(t => (portStr, t))
          }
          .mapAsync(1) {
            case (port, terminal) =>
              exportCsvService.getPortResponseForTerminal(startDate, endDate, portRegion.name, port, terminal.toString)
          }.flatMapConcat {
            case portResponse =>
              portResponse.map(pr => pr.httpResponse.entity.dataBytes
                .map {
                  _
                    .utf8String
                    .split("\n")
                    .filterNot(_.contains("ICAO"))
                    .map(line => s"${region.name},${pr.port},${pr.terminal},$line")
                    .mkString("\n")
                }).getOrElse(Source.empty)
          }.prepend(Source.single(headings)))
    }
  }

  val contentType = ContentTypes.`text/plain(UTF-8)`

  implicit val streamingSupport: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withContentType(contentType)

}
