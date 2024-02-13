package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import org.slf4j.LoggerFactory
import spray.json.enrichAny
import uk.gov.homeoffice.drt.jsonformats.PassengersSummaryFormat.JsonFormat
import uk.gov.homeoffice.drt.models.{PassengersSummaries, PassengersSummary}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.time.LocalDate
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success


object PassengerRoutes {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(httpClient: HttpClient)
           (implicit ec: ExecutionContext, mat: Materializer): Route =
    pathPrefix("passengers" / Segment / Segment) {
      case (startDate, endDate) =>
        parameters("port-codes") { portCodesStr =>
          val portCodes = portCodesStr.split(",")
          concat(
            pathEnd(
              passengersForPort(httpClient, portCodes, startDate, endDate, None)
            ),
            path(Segment)(terminal =>
              passengersForPort(httpClient, portCodes, startDate, endDate, Some(terminal))
            )
          )
        }
    }

  private def passengersForPort(httpClient: HttpClient, portCodes: Iterable[String], startDate: String, endDate: String, maybeTerminal: Option[String])
                               (implicit ec: ExecutionContext, mat: Materializer): Route = {
    get {
      extractRequest { request =>
        parameters("granularity".optional) { maybeGranularity =>
          val requests = for {
            start <- LocalDate.parse(startDate).toList
            end <- LocalDate.parse(endDate).toList
            portCode <- portCodes
          } yield {
            val incomingHeaders = request.headers
            val url = endpointUrl(PortCode(portCode), start, end, maybeTerminal, maybeGranularity.getOrElse("total"))

            HttpRequest(uri = url, headers = incomingHeaders)
          }

          val contentType = request.headers.find(_.name() == "Accept").map {
            case header if header.value() == "text/csv" => ContentTypes.`text/csv(UTF-8)`
            case _ => ContentTypes.`application/json`
          }.getOrElse(ContentTypes.`application/json`)

          val eventualResult: Future[PassengersSummaries] = requestsToPassengerSummaries(httpClient)(requests)

          import uk.gov.homeoffice.drt.jsonformats.PassengersSummaryFormat._

          onComplete(eventualResult) {
            case Success(result) =>
              val json = result.summaries.toJson
              val print1 = json.compactPrint
              complete(HttpEntity(contentType, print1))
          }
        }
      }
    }
  }

  def requestsToPassengerSummaries(httpClient: HttpClient)
                                  (requests: Iterable[HttpRequest])
                                  (implicit ec: ExecutionContext, mat: Materializer): Future[PassengersSummaries] = {
    import spray.json.DefaultJsonProtocol._
    import spray.json._

    Source(requests.toList)
      .mapAsync(1)(httpClient.send(_))
      .flatMapConcat { response =>
        response.status match {
          case StatusCodes.OK =>
            response.entity.dataBytes
          case _ =>
            log.error(s"Failed to get passenger summaries: ${response.status}")
            Source.empty[ByteString]
        }
      }
      .fold(PassengersSummaries.empty) {
        case (acc, dataBytes) =>
          acc ++ dataBytes.utf8String.parseJson.convertTo[Seq[PassengersSummary]]
      }
      .runWith(Sink.head)
  }

  private def endpointUrl(rootDomain: PortCode, start: LocalDate, end: LocalDate, maybeTerminal: Option[String], granularity: String): String = {
    val possibleTerminal = maybeTerminal.map(_.toLowerCase).toList

    val urlParts = Seq(
      Dashboard.drtInternalUriForPortCode(rootDomain),
      "api",
      "passengers",
      start.toISOString,
      end.toISOString,
    ) ++ possibleTerminal

    urlParts.mkString("/") + s"?granularity=$granularity"
  }
}
