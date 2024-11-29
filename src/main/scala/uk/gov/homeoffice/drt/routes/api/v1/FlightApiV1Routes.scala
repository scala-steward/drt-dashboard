package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory
import spray.json._
import uk.gov.homeoffice.drt.auth.Roles.ApiFlightAccess
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.services.api.v1.FlightExport.PortFlightsJson
import uk.gov.homeoffice.drt.services.api.v1.serialiser.FlightApiV1JsonFormats
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.Future
import scala.util.{Failure, Success}


object FlightApiV1Routes extends DefaultJsonProtocol with FlightApiV1JsonFormats {
  private val log = LoggerFactory.getLogger(getClass)

  case class FlightJsonResponse(startTime: String, endTime: String, ports: Seq[PortFlightsJson])

  def apply(enabledPorts: Iterable[PortCode],
            dateRangeJsonForPorts: Seq[PortCode] => (SDateLike, SDateLike) => Future[FlightJsonResponse]): Route =
    AuthByRole(ApiFlightAccess) {
      (get & path("flights")) {
        pathEnd(
          headerValueByName("X-Forwarded-Email") { email =>
            headerValueByName("X-Forwarded-Groups") { groups =>
              parameters("start", "end") { (startStr, endStr) =>
                val user = User.fromRoles(email, groups)
                val ports = enabledPorts.filter(user.accessiblePorts.contains(_)).toList
                val dateRangeJson = dateRangeJsonForPorts(ports)

                val start = SDate(startStr)
                val end = SDate(endStr)

                onComplete(dateRangeJson(start, end)) {
                  case Success(value) => complete(value.toJson.compactPrint)
                  case Failure(t) =>
                    log.error(s"Failed to get export: ${t.getMessage}")
                    complete(InternalServerError)
                }
              }
            }
          }
        )
      }
    }
}
