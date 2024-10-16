package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.exports.{ExportPort, ExportType}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.time.{LocalDate, SDate, SDateLike}
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.ExecutionContext


object QueueApiRoutes {
  private val log = LoggerFactory.getLogger(getClass)

  case class ExportRequest(exportType: ExportType, ports: Seq[ExportPort], startDate: LocalDate, endDate: LocalDate)

  implicit val csvStreaming: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withFramingRenderer(Flow[ByteString])
  implicit val csvMarshaller: ToEntityMarshaller[ByteString] =
    Marshaller.withFixedContentType(ContentTypes.`text/csv(UTF-8)`) { bytes =>
      HttpEntity(ContentTypes.`text/csv(UTF-8)`, bytes)
    }

  def apply(httpClient: HttpClient,
            destinationPorts: Iterable[PortCode],
            now: () => SDateLike,
            rootUrl: String,
           )
           (implicit ec: ExecutionContext, mat: Materializer): Route =
    (get & path("queues")) {
      pathEnd(
        headerValueByName("X-Forwarded-Email") { email =>
          headerValueByName("X-Forwarded-Groups") { groups =>
            parameters("start", "end", "period-minutes".as[Int].withDefault(15)) { (startStr, endStr, periodMinutes) =>
              val start = SDate(startStr)
              val end = SDate(endStr)
              val parallelism = 10

              val eventualContent = Source(destinationPorts.toSeq)
                .mapAsync(parallelism) { portCode =>
                  val uri = s"${Dashboard.drtInternalUriForPortCode(portCode)}/api/v1/queues?start=${start.toISOString}&${end.toISOString}&period-minutes=$periodMinutes"
                  val request = HttpRequest(uri = uri, headers = Seq(RawHeader("X-Forwarded-Email", email), RawHeader("X-Forwarded-Groups", groups)))
                  httpClient.send(request)
                }
                .mapAsync(1) { response =>
                  response.entity.dataBytes
                    .runFold(ByteString.empty)(_ ++ _)
                    .map(_.utf8String)
                }
                .runWith(Sink.seq)
                .map(ports =>
                  s"""{
                     |  "startTime": "$startStr",
                     |  "endTime": "$endStr",
                     |  "periodLengthMinutes": $periodMinutes,
                     |  "ports": [${ports.mkString(",")}]
                     |}""".stripMargin
                )
              onComplete(eventualContent) {
                case scala.util.Success(content) => complete(content)
                case scala.util.Failure(e) =>
                  log.error("Failed to get export", e)
                  complete("Failed to get export")
              }
            }
          }
        }
      )
    }
}
