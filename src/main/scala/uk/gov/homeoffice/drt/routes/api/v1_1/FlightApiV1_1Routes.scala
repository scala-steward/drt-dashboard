package uk.gov.homeoffice.drt.routes.api.v1_1

import uk.gov.homeoffice.drt.ports.Queues
import org.apache.pekko.http.scaladsl.model.StatusCodes.InternalServerError
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.slf4j.LoggerFactory
import spray.json._
import uk.gov.homeoffice.drt.arrivals.ApiFlightWithSplits
import uk.gov.homeoffice.drt.auth.Roles.ApiFlightAccess
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.services.AirportInfoService
import uk.gov.homeoffice.drt.services.api.v1_1.serialiser.FlightApiV1_1JsonFormats
import uk.gov.homeoffice.drt.splits.ApiSplitsToSplitRatio
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


object FlightApiV1_1Routes extends DefaultJsonProtocol with FlightApiV1_1JsonFormats {
  private val log = LoggerFactory.getLogger(getClass)

  case class FlightJsonV1_1(arrivalPortCode: String,
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
                            queuePaxCounts: Option[Map[String, Int]],
                       )

  object FlightJsonV1_1 {
    def apply(portCode: PortCode, fws: ApiFlightWithSplits)
             (implicit sourceOrderPreference: List[FeedSource]): FlightJsonV1_1 = {
      val ar = fws.apiFlight
      val queuePaxCounts: Option[Map[String, Int]] =
        ApiSplitsToSplitRatio
          .paxPerQueueUsingBestSplitsAsRatio(fws, sourceOrderPreference)
          .map(paxCounts => paxCounts.map { case (queue, count) => Queues.displayName(queue) -> count })
      FlightJsonV1_1(
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
        queuePaxCounts = queuePaxCounts,
      )
    }
  }

  case class FlightJsonResponseV1_1(periodStart: SDateLike, periodEnd: SDateLike, flights: Seq[FlightJsonV1_1])

  def apply(enabledPorts: Iterable[PortCode],
            dateRangeJsonForPorts: Seq[PortCode] => (SDateLike, SDateLike) => Future[FlightJsonResponseV1_1]): Route =
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
                    log.error(s"Failed to get export: ${t.getMessage}", t)
                    complete(InternalServerError)
                }
              }
            }
          }
        )
      }
    }
}
