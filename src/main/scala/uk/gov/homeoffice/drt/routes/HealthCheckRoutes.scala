package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.healthchecks.{HealthCheck, IncidentPriority}
import uk.gov.homeoffice.drt.json.HealthCheckAlarmJsonFormats
import uk.gov.homeoffice.drt.ports.PortCode

import scala.concurrent.Future
import scala.util.Success


trait HealthCheckJsonFormats extends DefaultJsonProtocol {
  implicit object IncidentPriorityJsonFormat extends RootJsonFormat[IncidentPriority] {
    override def read(json: JsValue): IncidentPriority = throw new NotImplementedError("Not implemented")

    override def write(obj: IncidentPriority): JsValue = obj.name.toJson
  }

  implicit object HealthCheckJsonFormat extends RootJsonFormat[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable] {
    override def read(json: JsValue): HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable =
      throw new NotImplementedError("Not implemented")

    override def write(obj: HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable): JsValue =
      JsObject(Map(
        "name" -> obj.name.toJson,
        "description" -> obj.description.toJson,
        "priority" -> obj.priority.toJson,
      ))
  }
}

object HealthCheckRoutes extends HealthCheckAlarmJsonFormats with HealthCheckJsonFormats {
  def apply(getAlarmStatuses: () => Future[Map[PortCode, Map[String, Boolean]]],
            healthChecks: Seq[HealthCheck[_ >: Double with Boolean <: AnyVal] with Serializable],
           ): Route =
    pathPrefix("health-checks") {
      concat(
        pathEnd {
          complete(healthChecks)
        },
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
