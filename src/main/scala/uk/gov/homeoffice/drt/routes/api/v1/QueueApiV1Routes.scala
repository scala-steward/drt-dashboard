package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.common.{CsvEntityStreamingSupport, EntityStreamingSupport}
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import spray.json.{DefaultJsonProtocol, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.auth.Roles.ApiQueueAccess
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.time.SDate
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.ExecutionContext


object QueueApiV1Routes extends DefaultJsonProtocol {
  private val log = LoggerFactory.getLogger(getClass)

  case class JsonResponse(startTime: String, endTime: String, periodLengthMinutes: Int, ports: Seq[String])

  implicit val jsonResponseFormat: RootJsonFormat[JsonResponse] = jsonFormat4(JsonResponse)

  implicit val csvStreaming: CsvEntityStreamingSupport = EntityStreamingSupport.csv().withFramingRenderer(Flow[ByteString])
  implicit val csvMarshaller: ToEntityMarshaller[ByteString] =
    Marshaller.withFixedContentType(ContentTypes.`text/csv(UTF-8)`) { bytes =>
      HttpEntity(ContentTypes.`text/csv(UTF-8)`, bytes)
    }

  def apply(httpClient: HttpClient, enabledPorts: Iterable[PortCode])
           (implicit ec: ExecutionContext, mat: Materializer): Route =
    AuthByRole(ApiQueueAccess) {
      (get & path("queues")) {
        pathEnd {
          headerValueByName("X-Forwarded-Email") { email =>
            headerValueByName("X-Forwarded-Groups") { groups =>
              val user = User.fromRoles(email, groups)

              val defaultSlotSizeMinutes = 15

              parameters("start", "end", "slot-size-minutes".as[Int].withDefault(defaultSlotSizeMinutes)) { (startStr, endStr, periodMinutes) =>
                val start = SDate(startStr)
                val end = SDate(endStr)
                val parallelism = 10

                val ports = enabledPorts.filter(user.accessiblePorts.contains(_)).toList

                val eventualContent = Source(ports)
                  .mapAsync(parallelism) { portCode =>
                    val uri = s"${Dashboard.drtInternalUriForPortCode(portCode)}/api/v1/queues?start=${start.toISOString}&end=${end.toISOString}&period-minutes=$periodMinutes"
                    val request = HttpRequest(uri = uri, headers = Seq(RawHeader("X-Forwarded-Email", email), RawHeader("X-Forwarded-Groups", groups)))
                    httpClient.send(request)
                  }
                  .mapAsync(1) { response =>
                    response.entity.dataBytes
                      .runFold(ByteString.empty)(_ ++ _)
                      .map(_.utf8String)
                  }
                  .runWith(Sink.seq)
                  .map(ports => JsonResponse(startStr, endStr, periodMinutes, ports).toJson.compactPrint)

                onComplete(eventualContent) {
                  case scala.util.Success(content) => complete(content)
                  case scala.util.Failure(e) =>
                    log.error(s"Failed to get export: ${e.getMessage}")
                    complete(InternalServerError)
                }
              }
            }
          }
        }
      }
    }
}
