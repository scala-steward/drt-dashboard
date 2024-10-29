package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import spray.json._
import uk.gov.homeoffice.drt.auth.Roles.ApiQueueAccess
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.api.v1.AuthApiV1Routes.JsonResponse
import uk.gov.homeoffice.drt.routes.api.v1.QueueApiV1Routes.QueueJsonResponse
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.ExecutionContext

trait QueueApiV1JsonFormats extends DefaultJsonProtocol {
  implicit object jsonResponseFormat extends RootJsonFormat[JsonResponse] {

    override def write(obj: JsonResponse): JsValue = obj match {
      case obj: QueueJsonResponse => JsObject(Map(
        "startTime" -> obj.startTime.toJson,
        "endTime" -> obj.endTime.toJson,
        "periodLengthMinutes" -> obj.slotSizeMinutes.toJson,
        "ports" -> JsArray(obj.ports.map(_.parseJson).toVector),
      ))
    }

    override def read(json: JsValue): JsonResponse = throw new Exception("Not implemented")
  }
}

object QueueApiV1Routes extends DefaultJsonProtocol with ApiV1Routes with QueueApiV1JsonFormats {
  case class QueueJsonResponse(startTime: String, endTime: String, slotSizeMinutes: Int, ports: Seq[String]) extends JsonResponse

  def apply(httpClient: HttpClient, enabledPorts: Iterable[PortCode])
           (implicit ec: ExecutionContext, mat: Materializer): Route =
    AuthByRole(ApiQueueAccess) {
      (get & path("queues")) {
        pathEnd {
          headerValueByName("X-Forwarded-Email") { email =>
            headerValueByName("X-Forwarded-Groups") { groups =>
              val defaultSlotSizeMinutes = 15

              parameters("start", "end", "slot-size-minutes".as[Int].withDefault(defaultSlotSizeMinutes)) { (startStr, endStr, slotSizeMinutes) =>
                val portUri: PortCode => String =
                  portCode => s"${Dashboard.drtInternalUriForPortCode(portCode)}/api/v1/queues?start=$startStr&end=$endStr&period-minutes=$slotSizeMinutes"

                val jsonResponse: (String, String, Seq[String]) => QueueJsonResponse =
                  (startTime, endTime, ports) => QueueJsonResponse(startTime, endTime, slotSizeMinutes, ports)

                multiPortResponse(httpClient, enabledPorts, email, groups, portUri, jsonResponse, startStr, endStr)
              }
            }
          }
        }
      }
    }
}
