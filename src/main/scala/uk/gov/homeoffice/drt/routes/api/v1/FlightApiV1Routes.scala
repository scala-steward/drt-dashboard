package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.LoggerFactory
import spray.json._
import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.auth.Roles.ApiFlightAccess
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.services.AirportInfoService
import uk.gov.homeoffice.drt.services.api.v1.serialiser.FlightApiV1JsonFormats
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


object FlightApiV1Routes extends DefaultJsonProtocol with FlightApiV1JsonFormats {
  private val log = LoggerFactory.getLogger(getClass)

  case class FlightJson(arrivalPortCode: String,
                        arrivalTerminal: String,
                        code: String,
                        originPortIata: String,
                        originPortName: String,
                        scheduledTime: Long,
                        estimatedLandingTime: Option[Long],
                        actualChocksTime: Option[Long],
                        estimatedPcpStartTime: Option[Long],
                        estimatedPcpEndTime: Option[Long],
                        estimatedPaxCount: Option[Int],
                        status: String,
                       )

  object FlightJson {
    def apply(portCode: PortCode, ar: Arrival)
             (implicit sourceOrderPreference: List[FeedSource]): FlightJson = FlightJson(
      arrivalPortCode = portCode.iata,
      arrivalTerminal = ar.Terminal.toString,
      code = ar.flightCodeString,
      originPortIata = ar.Origin.iata,
      originPortName = AirportInfoService.airportInfo(ar.Origin).map(_.airportName).getOrElse("n/a"),
      scheduledTime = ar.Scheduled,
      estimatedLandingTime = ar.Estimated,
      actualChocksTime = ar.ActualChox,
      estimatedPcpStartTime = Try(ar.pcpRange(sourceOrderPreference).min).toOption,
      estimatedPcpEndTime = Try(ar.pcpRange(sourceOrderPreference).max).toOption,
      estimatedPaxCount = ar.bestPcpPaxEstimate(sourceOrderPreference),
      status = ar.displayStatus.description,
    )
  }

  case class FlightJsonResponse(periodStart: SDateLike, periodEnd: SDateLike, flights: Seq[FlightJson])

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
