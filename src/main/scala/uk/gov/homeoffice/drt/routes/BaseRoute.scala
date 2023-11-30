package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete}
import akka.http.scaladsl.server.{Route, StandardRoute}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait BaseRoute {
  val log: Logger = LoggerFactory.getLogger(getClass)

  def routeResponse(responseF: Future[StandardRoute], eventText: String): Route = {
    onComplete(responseF) {
      case Success(result) => result
      case Failure(ex) =>
        log.error(s"Error $eventText", ex)
        complete(StatusCodes.InternalServerError, ex.getMessage)
    }
  }
}
