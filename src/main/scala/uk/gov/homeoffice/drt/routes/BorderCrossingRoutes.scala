package uk.gov.homeoffice.drt.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{complete, onComplete, pathPrefix, storeUploadedFile, withRequestTimeout}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import uk.gov.homeoffice.drt.auth.Roles.ManageUsers
import uk.gov.homeoffice.drt.db.tables.{BorderCrossing, GateType}
import uk.gov.homeoffice.drt.ports.PortCode
import uk.gov.homeoffice.drt.ports.Terminals.Terminal
import uk.gov.homeoffice.drt.routes.services.AuthByRole
import uk.gov.homeoffice.drt.services.bx.ImportBorderCrossings

import java.io.File
import java.nio.file.Files
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


object BorderCrossingRoutes {
  private val log = LoggerFactory.getLogger(getClass)

  private def tempDestination(fileInfo: FileInfo): File =
    Files.createTempFile(fileInfo.fileName, ".tmp").toFile

  def apply(replaceHoursForPortTerminal: (PortCode, Terminal, GateType, Iterable[BorderCrossing]) => Future[Int])
           (implicit mat: Materializer): Route = {

    val importFile: String => Future[Int] = ImportBorderCrossings(replaceHoursForPortTerminal)

    pathPrefix("border-crossing") {
      AuthByRole(ManageUsers) {
        withRequestTimeout(2.minutes) {
          storeUploadedFile("excel", tempDestination) {
            case (_, file) =>
              val eventualDone = importFile(file.getPath).map { insertCount =>
                log.info(s"Imported $insertCount border crossings")
                if (file.delete()) log.info("Temporary file deleted")
                else log.error(s"Failed to delete temporary file ${file.getPath}")
                insertCount
              }

              onComplete(eventualDone) {
                case Success(insertCount) => complete("""{"inserted": """ + insertCount + """}""")
                case Failure(error) =>
                  log.error(s"Error importing border crossings: ${error.getMessage}", error)
                  complete(StatusCodes.InternalServerError)
              }
          }
        }
      }
    }
  }
}
