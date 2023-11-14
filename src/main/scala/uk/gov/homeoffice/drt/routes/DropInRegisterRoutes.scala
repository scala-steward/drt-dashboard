package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import org.slf4j.{Logger, LoggerFactory}
import spray.json.{RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.db.{DropInRegistrationDao, DropInRegistrationRow}
import uk.gov.homeoffice.drt.json.DefaultTimeJsonProtocol

import scala.concurrent.{ExecutionContext, Future}

trait DropInRegistrationJsonFormats extends DefaultTimeJsonProtocol {
  implicit val dropInRegistrationRowFormatParser: RootJsonFormat[DropInRegistrationRow] = jsonFormat4(DropInRegistrationRow)
}

object DropInRegisterRoutes extends BaseRoute with DropInRegistrationJsonFormats {
  override val log: Logger = LoggerFactory.getLogger(getClass)

  def removeRegisteredUser(dropInRegistrationDao: DropInRegistrationDao)(implicit ec: ExecutionContext): Route =
    delete {
      path(Segment / Segment) { (dropInId, email) =>
        val removedUserResult = dropInRegistrationDao.removeRegisteredUser(dropInId, email)
        routeResponse(removedUserResult.map(_ => complete(StatusCodes.OK, s"User $email is removed from dr successfully")), "Removing User from Drop-In")
      }
    }

  def getRegisteredUsers(dropInRegistrationDao: DropInRegistrationDao)(implicit ec: ExecutionContext): Route =
    get {
      path(Segment) { seminarId =>
        val registeredUsersResult: Future[Seq[DropInRegistrationRow]] = dropInRegistrationDao.getRegisteredUsers(seminarId)
        routeResponse(registeredUsersResult.map(forms => complete(StatusCodes.OK, forms.toJson)), "Getting registered drop-in users")
      }
    }

  def apply(dropInRegistrationDao: DropInRegistrationDao)(implicit ec: ExecutionContext): Route =
    pathPrefix("drop-in-register") {
      concat(
        getRegisteredUsers(dropInRegistrationDao),
        removeRegisteredUser(dropInRegistrationDao),
      )
    }
}
