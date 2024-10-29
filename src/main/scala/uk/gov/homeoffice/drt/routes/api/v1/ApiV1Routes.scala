package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import spray.json._
import uk.gov.homeoffice.drt.HttpClient
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.api.v1.AuthApiV1Routes.JsonResponse

import scala.concurrent.ExecutionContext

trait ApiV1Routes {
  private val log = LoggerFactory.getLogger(getClass)


  def multiPortResponse(httpClient: HttpClient,
                        enabledPorts: Iterable[PortCode],
                        email: String,
                        groups: String,
                        portUri: PortCode => String,
                        jsonResponse: (String, String, Seq[String]) => JsonResponse,
                        startStr: String,
                        endStr: String,
                       )
                       (implicit ec: ExecutionContext, mat: Materializer, jsonWriter: JsonWriter[JsonResponse]): Route = {
    val parallelism = 10

    val user = User.fromRoles(email, groups)
    val ports = enabledPorts.filter(user.accessiblePorts.contains(_)).toList

    val eventualContent = Source(ports)
      .mapAsync(parallelism) { portCode =>
        val request = HttpRequest(uri = portUri(portCode), headers = Seq(RawHeader("X-Forwarded-Email", email), RawHeader("X-Forwarded-Groups", groups)))
        httpClient.send(request)
      }
      .mapAsync(1) { response =>
        response.entity.dataBytes
          .runFold(ByteString.empty)(_ ++ _)
          .map(_.utf8String)
      }
      .runWith(Sink.seq)
      .map(ports => jsonResponse(startStr, endStr, ports).toJson.compactPrint)

    onComplete(eventualContent) {
      case scala.util.Success(content) => complete(content)
      case scala.util.Failure(e) =>
        log.error(s"Failed to get export: ${e.getMessage}")
        complete(InternalServerError)
    }
  }

}
