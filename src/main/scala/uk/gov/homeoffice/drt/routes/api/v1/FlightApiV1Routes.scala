package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import spray.json._
import uk.gov.homeoffice.drt.auth.Roles.ApiFlightAccess
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.api.v1.AuthApiV1Routes.JsonResponse
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.ExecutionContext


trait FlightApiV1JsonFormats extends DefaultJsonProtocol {
  implicit object jsonResponseFormat extends RootJsonFormat[JsonResponse] {

    override def write(obj: JsonResponse): JsValue = JsObject(Map(
      "startTime" -> obj.startTime.toJson,
      "endTime" -> obj.endTime.toJson,
      "ports" -> JsArray(obj.ports.map(_.parseJson).toVector),
    ))

    override def read(json: JsValue): JsonResponse = throw new Exception("Not implemented")
  }
}

object FlightApiV1Routes extends DefaultJsonProtocol with ApiV1Routes with FlightApiV1JsonFormats {

  case class FlightJsonResponse(startTime: String, endTime: String, ports: Seq[String]) extends JsonResponse

  def apply(httpClient: HttpClient, enabledPorts: Iterable[PortCode])
           (implicit ec: ExecutionContext, mat: Materializer): Route =
    AuthByRole(ApiFlightAccess) {
      (get & path("flights")) {
        pathEnd(
          headerValueByName("X-Forwarded-Email") { email =>
            headerValueByName("X-Forwarded-Groups") { groups =>
              parameters("start", "end") { (startStr, endStr) =>
                val portUri: PortCode => String =
                  portCode => s"${Dashboard.drtInternalUriForPortCode(portCode)}/api/v1/flights?start=$startStr&end=$endStr"
                val jsonResponse: (String, String, Seq[String]) => JsonResponse =
                  (startTime, endTime, ports) => FlightJsonResponse(startTime, endTime, ports)

                multiPortResponse(httpClient, enabledPorts, email, groups, portUri, jsonResponse, startStr, endStr)
              }
            }
          }
        )
      }
    }
}
