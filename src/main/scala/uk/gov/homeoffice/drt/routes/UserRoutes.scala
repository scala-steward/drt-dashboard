package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.server.Directives._
import spray.json.enrichAny
import uk.gov.homeoffice.drt.JsonSupport
import uk.gov.homeoffice.drt.db.UserJsonSupport
import uk.gov.homeoffice.drt.services.UserService

import scala.concurrent.ExecutionContextExecutor
import scala.util.{ Failure, Success }

object UserRoutes extends JsonSupport with UserJsonSupport {
  def apply(prefix: String, userService: UserService)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]) = {
    pathPrefix(prefix) {
      (get & path("all")) {
        headerValueByName("X-Auth-Roles") { _ =>
          onComplete(userService.getUsers()) {
            case Success(value) =>
              complete(value.toJson)
            case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }
  }
}

