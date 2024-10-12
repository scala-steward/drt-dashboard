package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.slf4j.{Logger, LoggerFactory}
import spray.json._
import uk.gov.homeoffice.drt._
import uk.gov.homeoffice.drt.alerts.{Alert, MultiPortAlert, MultiPortAlertClient, MultiPortAlertJsonSupport}
import uk.gov.homeoffice.drt.auth.Roles._
import uk.gov.homeoffice.drt.authentication._
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.routes.services.AuthByRole

import scala.concurrent.{ExecutionContextExecutor, Future}

case class PortAlerts(portCode: String, alerts: List[Alert])

object AlertsRoutes extends MultiPortAlertJsonSupport
  with UserJsonSupport
  with ClientConfigJsonFormats
  with ClientUserAccessDataJsonSupport {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def apply()
           (implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route =
    concat(
      (post & path("alerts")) {
        AuthByRole(CreateAlerts) {
          headerValueByName("X-Forwarded-Groups") { rolesStr =>
            headerValueByName("X-Forwarded-Email") { email =>
              entity(as[MultiPortAlert]) { multiPortAlert =>
                val user = User.fromRoles(email, rolesStr)
                val allPorts = PortRegion.ports.map(_.iata)
                val futureResponses = MultiPortAlertClient.saveAlertsForPorts(allPorts, multiPortAlert, user)
                complete(Future.sequence(futureResponses).map(_ => StatusCodes.Created))
              }
            }
          }
        }
      },
      (get & path("alerts")) {
        AuthByRole(CreateAlerts) {
          headerValueByName("X-Forwarded-Groups") { rolesStr =>
            headerValueByName("X-Forwarded-Email") { email =>
              val user = User.fromRoles(email, rolesStr)

              val futurePortAlerts: Seq[Future[PortAlerts]] = user.accessiblePorts
                .map { portCode =>
                  DashboardClient.getWithRoles(s"${Dashboard.drtInternalUriForPortCode(PortCode(portCode))}/alerts/0", user.roles)
                    .flatMap { res =>
                      Unmarshal[HttpEntity](res.entity.withContentType(ContentTypes.`application/json`))
                        .to[List[Alert]]
                        .map(alerts => PortAlerts(portCode, alerts))
                        .recover {
                          case e: Throwable =>
                            log.error(s"Failed to unmarshall json alerts for $portCode", e)
                            PortAlerts(portCode, List())
                        }
                    }
                    .recover {
                      case t =>
                        log.error(s"Failed to retrieve alerts for $portCode at ${Dashboard.drtInternalUriForPortCode(PortCode(portCode))}/alerts/0", t)
                        PortAlerts(portCode, List())
                    }
                }
                .toList

              val eventualValue = Future.sequence(futurePortAlerts).map(_.toJson)

              complete(eventualValue)
            }
          }
        }
      },
      (delete & path("alerts" / Segment)) { port =>
        AuthByRole(CreateAlerts) {
          headerValueByName("X-Forwarded-Groups") { rolesStr =>
            headerValueByName("X-Forwarded-Email") { email =>
              val user = User.fromRoles(email, rolesStr)
              val deleteEndpoint = s"${Dashboard.drtInternalUriForPortCode(PortCode(port))}/alerts"
              complete(DashboardClient.deleteWithRoles(deleteEndpoint, user.roles).map { res =>
                res.status
              })
            }
          }
        }
      }
    )
}
