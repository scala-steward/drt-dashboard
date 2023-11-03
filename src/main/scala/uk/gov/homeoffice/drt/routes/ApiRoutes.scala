package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.{Directive0, Route}
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.slf4j.{Logger, LoggerFactory}
import spray.json._
import uk.gov.homeoffice.drt.alerts.{Alert, MultiPortAlert, MultiPortAlertClient, MultiPortAlertJsonSupport}
import uk.gov.homeoffice.drt.auth.Roles._
import uk.gov.homeoffice.drt.authentication._
import uk.gov.homeoffice.drt.healthchecks.ScheduledPause
import uk.gov.homeoffice.drt.json.ScheduledPauseJsonFormats.scheduledPauseJsonFormat
import uk.gov.homeoffice.drt.persistence.ScheduledPausePersistence
import uk.gov.homeoffice.drt.ports.{PortCode, PortRegion}
import uk.gov.homeoffice.drt.services.UserService
import uk.gov.homeoffice.drt._
import uk.gov.homeoffice.drt.time.SDate

import java.sql.Timestamp
import java.util.Date
import scala.compat.java8.OptionConverters._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

case class PortAlerts(portCode: String, alerts: List[Alert])

object ApiRoutes extends MultiPortAlertJsonSupport
  with UserJsonSupport
  with ClientConfigJsonFormats
  with ClientUserAccessDataJsonSupport {

  val log: Logger = LoggerFactory.getLogger(getClass)

  def authByRole(role: Role): Directive0 = authorize(ctx => {
    (for {
      rolesHeader <- ctx.request.getHeader("X-Auth-Roles").asScala
      emailHeader <- ctx.request.getHeader("X-Auth-Email").asScala
    } yield User.fromRoles(emailHeader.value(), rolesHeader.value())) match {
      case Some(user) => user.hasRole(role)
      case None => false
    }
  })

  def apply(prefix: String,
            clientConfig: ClientConfig,
            userService: UserService,
            scheduledPausePersistence: ScheduledPausePersistence,
           )
           (implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route =
    pathPrefix(prefix) {
      concat(
        (get & path("user")) {
          headerValueByName("X-Auth-Roles") { rolesStr =>
            headerValueByName("X-Auth-Email") { email =>
              complete(User.fromRoles(email, rolesStr))
            }
          }
        },
        (get & path("track-user")) {
          headerValueByName("X-Auth-Email") { email =>
            optionalHeaderValueByName("X-Auth-Username") { usernameOption =>
              userService.upsertUser(
                uk.gov.homeoffice.drt.db.User(
                  id = usernameOption.getOrElse(email),
                  username = usernameOption.getOrElse(email),
                  email = email,
                  drop_in_notification_at = None,
                  latest_login = new Timestamp(new Date().getTime),
                  inactive_email_sent = None,
                  revoked_access = None,
                  created_at = Some(new Timestamp(new Date().getTime))), Some("userTracking"))
              complete(StatusCodes.OK)
            }
          }
        },
        (get & path("config")) {
          headerValueByName("X-Auth-Roles") { _ =>
            complete(clientConfig)
          }
        },
        (post & path("health-check-pauses")) {
          authByRole(HealthChecksEdit) {
            entity(as[ScheduledPause]) { scheduledPause =>
              log.info(s"Received health check pause to save")
              handleFutureOperation(scheduledPausePersistence.insert(scheduledPause), "Failed to save health check pause")
            }
          }
        },
        (get & path("health-check-pauses")) {
          authByRole(HealthChecksEdit) {
            complete(scheduledPausePersistence.get(Option(SDate.now().millisSinceEpoch)))
          }
        },
        (delete & path("health-check-pauses" / Segment / Segment)) { (from, to) =>
          val fromMillis = from.toLong
          val toMillis = to.toLong
          authByRole(HealthChecksEdit) {
            log.info(s"Received health check pause to delete")
            handleFutureOperation(scheduledPausePersistence.delete(fromMillis, toMillis), "Failed to delete health check pause")
          }
        },
        (post & path("alerts")) {
          authByRole(CreateAlerts) {
            headerValueByName("X-Auth-Roles") { rolesStr =>
              headerValueByName("X-Auth-Email") { email =>
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
          authByRole(CreateAlerts) {
            headerValueByName("X-Auth-Roles") { rolesStr =>
              headerValueByName("X-Auth-Email") { email =>
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
          authByRole(CreateAlerts) {
            headerValueByName("X-Auth-Roles") { rolesStr =>
              headerValueByName("X-Auth-Email") { email =>
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

  private def handleFutureOperation(eventual: Future[_], errorMsg: String)
                                   (implicit ec: ExecutionContext): Route =
    onComplete(eventual) {
      case Success(_) => complete(Future(StatusCodes.OK))
      case Failure(t) =>
        log.error(errorMsg, t)
        complete(StatusCodes.InternalServerError)
    }
}
