package uk.gov.homeoffice.drt.routes.api.v1

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import org.slf4j.LoggerFactory
import spray.json._
import uk.gov.homeoffice.drt.Server.paxFeedSourceOrder
import uk.gov.homeoffice.drt.arrivals.Arrival
import uk.gov.homeoffice.drt.auth.Roles.ApiFlightAccess
import uk.gov.homeoffice.drt.authentication.User
import uk.gov.homeoffice.drt.db.AppDatabase
import uk.gov.homeoffice.drt.db.dao.FlightDao
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.ports.config.AirportConfigs
import uk.gov.homeoffice.drt.ports.{FeedSource, PortCode}
import uk.gov.homeoffice.drt.routes.api.v1.FlightApiV1Routes.FlightJsonResponse
import uk.gov.homeoffice.drt.routes.api.v1.FlightExport.{FlightJson, PortFlightsJson, TerminalFlightsJson}
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.services.AirportInfoService
import uk.gov.homeoffice.drt.time.{SDate, SDateLike}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


trait FlightApiV1JsonFormats extends DefaultJsonProtocol {
  implicit object FlightJsonJsonFormat extends RootJsonFormat[FlightJson] {
    override def write(obj: FlightJson): JsValue = {
      val maybePax = obj.estimatedPaxCount.filter(_ > 0)
      JsObject(
        "code" -> obj.code.toJson,
        "originPortIata" -> obj.originPortIata.toJson,
        "originPortName" -> obj.originPortName.toJson,
        "scheduledTime" -> SDate(obj.scheduledTime).toISOString.toJson,
        "estimatedLandingTime" -> obj.estimatedLandingTime.map(SDate(_).toISOString).toJson,
        "actualChocksTime" -> obj.actualChocksTime.map(SDate(_).toISOString).toJson,
        "estimatedPcpStartTime" -> maybePax.flatMap(_ => obj.estimatedPcpStartTime.map(SDate(_).toISOString)).toJson,
        "estimatedPcpEndTime" -> maybePax.flatMap(_ => obj.estimatedPcpEndTime.map(SDate(_).toISOString)).toJson,
        "estimatedPcpPaxCount" -> obj.estimatedPaxCount.toJson,
        "status" -> obj.status.toJson
      )
    }

    override def read(json: JsValue): FlightJson = json match {
      case JsObject(fields) => FlightJson(
        fields.get("code").map(_.convertTo[String]).getOrElse(""),
        fields.get("originPortIata").map(_.convertTo[String]).getOrElse(""),
        fields.get("originPortName").map(_.convertTo[String]).getOrElse(""),
        fields.get("scheduledTime").map(_.convertTo[Long]).getOrElse(0L),
        fields.get("estimatedLandingTime").map(_.convertTo[Long]),
        fields.get("actualChocksTime").map(_.convertTo[Long]),
        fields.get("estimatedPcpStartTime").map(_.convertTo[Long]),
        fields.get("estimatedPcpEndTime").map(_.convertTo[Long]),
        fields.get("estimatedPcpPaxCount").map(_.convertTo[Int]),
        fields.get("status").map(_.convertTo[String]).getOrElse(""),
      )
      case unexpected => throw new Exception(s"Failed to parse FlightJson. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val flightJsonFormat: RootJsonFormat[FlightJson] = jsonFormat10(FlightJson.apply)

  implicit object TerminalJsonFormat extends RootJsonFormat[Terminal] {
    override def write(obj: Terminal): JsValue = obj.toString.toJson

    override def read(json: JsValue): Terminal = json match {
      case JsString(value) => Terminal(value)
      case unexpected => throw new Exception(s"Failed to parse Terminal. Expected JsString. Got ${unexpected.getClass}")
    }
  }

  implicit val terminalFlightsJsonFormat: RootJsonFormat[TerminalFlightsJson] = jsonFormat2(TerminalFlightsJson.apply)

  implicit object PortCodeJsonFormat extends RootJsonFormat[PortCode] {
    override def write(obj: PortCode): JsValue = obj.iata.toJson

    override def read(json: JsValue): PortCode = json match {
      case JsString(value) => PortCode(value)
      case unexpected => throw new Exception(s"Failed to parse Terminal. Expected JsString. Got ${unexpected.getClass}")
    }
  }


  implicit val portFlightsJsonFormat: RootJsonFormat[PortFlightsJson] = jsonFormat2(PortFlightsJson.apply)
  implicit object jsonResponseFormat extends RootJsonFormat[FlightJsonResponse] {

    override def write(obj: FlightJsonResponse): JsValue = JsObject(Map(
      "startTime" -> obj.startTime.toJson,
      "endTime" -> obj.endTime.toJson,
      "ports" -> obj.ports.toJson,
    ))

    override def read(json: JsValue): FlightJsonResponse = throw new Exception("Not implemented")
  }
}

object FlightApiV1Routes extends DefaultJsonProtocol with FlightApiV1JsonFormats {
  private val log = LoggerFactory.getLogger(getClass)

  case class FlightJsonResponse(startTime: String, endTime: String, ports: Seq[PortFlightsJson])

  def apply(enabledPorts: Iterable[PortCode],
            arrivalSource: Seq[PortCode] => (SDateLike, SDateLike) => Future[FlightJsonResponse]): Route =
    AuthByRole(ApiFlightAccess) {
      (get & path("flights")) {
        pathEnd(
          headerValueByName("X-Forwarded-Email") { email =>
            headerValueByName("X-Forwarded-Groups") { groups =>
              parameters("start", "end") { (startStr, endStr) =>
                val user = User.fromRoles(email, groups)
                val ports = enabledPorts.filter(user.accessiblePorts.contains(_)).toList
                val flights = arrivalSource(ports)

                val start = SDate(startStr)
                val end = SDate(endStr)

                onComplete(flights(start, end)) {
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

object FlightExport {
  case class FlightJson(code: String,
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
    def apply(ar: Arrival)
             (implicit sourceOrderPreference: List[FeedSource]): FlightJson = FlightJson(
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

  case class TerminalFlightsJson(terminal: Terminal, flights: Iterable[FlightJson])

  case class PortFlightsJson(portCode: PortCode, terminals: Iterable[TerminalFlightsJson])

  def flights(db: AppDatabase)
             (implicit ec: ExecutionContext, mat: Materializer): Seq[PortCode] => (SDateLike, SDateLike) => Future[FlightJsonResponse] =
    portCodes => (start, end) => {
      val dates = Set(start.toLocalDate, end.toLocalDate)
      val flightDao = FlightDao()

      Source(portCodes)
        .mapAsync(1) { portCode =>
          val eventualPortFlights = AirportConfigs.confByPort(portCode).terminals.map { terminal =>
            implicit val sourceOrder: List[FeedSource] = paxFeedSourceOrder(portCode)
            val flightsForDatesAndTerminals = flightDao.flightsForPcpDateRange(portCode, sourceOrder, db.run)

            flightsForDatesAndTerminals(dates.min, dates.max, Seq(terminal))
              .runWith(Sink.seq)
              .map { r =>
                val relevantFlights = r.flatMap { case (_, flights) =>
                  flights
                    .filter(_.apiFlight.hasPcpDuring(start, end, sourceOrder))
                    .map(f => FlightJson(f.apiFlight))
                }
                TerminalFlightsJson(terminal, relevantFlights)
              }
          }

          Future
            .sequence(eventualPortFlights)
            .map(PortFlightsJson(portCode, _))
        }
        .runWith(Sink.seq)
        .map(FlightJsonResponse(start.toISOString, end.toISOString, _))
    }
}
