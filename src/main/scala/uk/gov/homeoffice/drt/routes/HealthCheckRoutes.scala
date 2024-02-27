package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.enrichAny
import uk.gov.homeoffice.drt.json.HealthCheckAlarmJsonFormats
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.Future
import scala.util.Success


object HealthCheckRoutes extends HealthCheckAlarmJsonFormats {
  def apply(getAlarmStatuses: () => Future[Map[PortCode, Map[String, Boolean]]]): Route =
    pathPrefix("health-checks") {
      concat(
        path("alarm-statuses") {
          onComplete(getAlarmStatuses()) {
            case Success(alarmStatuses) =>
              complete(alarmStatuses.toJson)
            case _ =>
              complete(StatusCodes.InternalServerError)
          }
        },
      )
    }
}
