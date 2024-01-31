package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import akka.util.ByteString
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.time.LocalDate
import uk.gov.homeoffice.drt.{Dashboard, HttpClient}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success


object PassengerRoutes {
  private val log = LoggerFactory.getLogger(getClass)

  def apply(httpClient: HttpClient)
           (implicit ec: ExecutionContext, mat: Materializer): Route =
    pathPrefix("passengers" / Segment / Segment / Segment) {
      case (portCode, startDate, endDate) =>
        concat(
          pathEnd(
            passengersForPort(httpClient, portCode, startDate, endDate, None)
          ),
          path(Segment)(terminal =>
            passengersForPort(httpClient, portCode, startDate, endDate, Some(terminal))
          )
        )
    }

  private def passengersForPort(httpClient: HttpClient, portCode: String, startDate: String, endDate: String, maybeTerminal: Option[String])
                               (implicit ec: ExecutionContext, mat: Materializer): Route = {
    get {
      extractRequest { request =>
        parameters("granularity".optional) { maybeGranularity =>
          val maybeRequest = for {
            start <- LocalDate.parse(startDate)
            end <- LocalDate.parse(endDate)
          } yield {
            val incomingHeaders = request.headers
            val url = endpointUrl(PortCode(portCode), start, end, maybeTerminal, maybeGranularity.getOrElse("total"))

            HttpRequest(uri = url, headers = incomingHeaders)
          }

          val contentType = request.headers.find(_.name() == "Accept").map {
            case header if header.value() == "text/csv" => ContentTypes.`text/csv(UTF-8)`
            case _ => ContentTypes.`application/json`
          }.getOrElse(ContentTypes.`application/json`)

          val eventualResult = maybeRequest match {
            case Some(httpRequest) =>
              httpClient.send(httpRequest).flatMap { response =>
                response.status match {
                  case StatusCodes.OK =>
                    response.entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
                      .map { body =>
                        complete(HttpEntity(contentType, body))
                      }
                      .recoverWith { t =>
                        log.error(s"Error while requesting passengers from ${httpRequest.uri}", t)
                        response.entity.discardBytes()
                        Future.successful(complete(InternalServerError))
                      }
                  case _ =>
                    Future.successful(complete(response.status))
                }
              }
            case None =>
              Future.successful(complete(StatusCodes.BadRequest))
          }
          onComplete(eventualResult) {
            case Success(result) => result
          }
        }
      }
    }
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
