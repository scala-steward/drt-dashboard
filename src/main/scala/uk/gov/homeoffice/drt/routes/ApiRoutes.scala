package uk.gov.homeoffice.drt.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpHeader.ParsingResult.Ok
import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, StatusCodes }
import akka.http.scaladsl.server.Directives.{ complete, pathPrefix, _ }
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.{ Directive0, Route }
import akka.http.scaladsl.unmarshalling.Unmarshal
import org.slf4j.{ Logger, LoggerFactory }
import spray.json.{ JsArray, JsObject, JsString }
import uk.gov.homeoffice.drt.alerts.{ Alert, MultiPortAlert, MultiPortAlertClient }
import uk.gov.homeoffice.drt.auth.Roles.{ CreateAlerts, RedListsEdit, Role }
import uk.gov.homeoffice.drt.authentication.{ AccessRequest, User }
import uk.gov.homeoffice.drt.notifications.EmailNotifications
import uk.gov.homeoffice.drt.redlist.SetRedListUpdate
import uk.gov.homeoffice.drt.{ Dashboard, DashboardClient }

import scala.compat.java8.OptionConverters._
import scala.concurrent.{ ExecutionContextExecutor, Future }
import scala.util.{ Failure, Success }

object ApiRoutes {
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

  def apply(
    prefix: String,
    portCodes: Array[String],
    domain: String,
    notifications: EmailNotifications,
    teamEmail: String)(implicit ec: ExecutionContextExecutor, system: ActorSystem[Nothing]): Route =
    pathPrefix(prefix) {
      concat(
        (get & path("user")) {
          headerValueByName("X-Auth-Roles") { rolesStr =>
            headerValueByName("X-Auth-Email") { email =>
              import uk.gov.homeoffice.drt.authentication.UserJsonSupport._

              complete(User.fromRoles(email, rolesStr))
            }
          }
        },
        (get & path("config")) {
          headerValueByName("X-Auth-Roles") { _ =>
            import uk.gov.homeoffice.drt.authentication.UserJsonSupport._
            val json = JsObject(Map(
              "ports" -> JsArray(portCodes.map(JsString(_)).toVector),
              "domain" -> JsString(domain),
              "teamEmail" -> JsString(teamEmail)))
            complete(json)
          }
        },
        (post & path("request-access")) {
          headerValueByName("X-Auth-Email") { userEmail =>
            import uk.gov.homeoffice.drt.authentication.AccessRequestJsonSupport._

            entity(as[AccessRequest]) { accessRequest =>
              val failures = notifications.sendRequest(userEmail, accessRequest).foldLeft(List[(String, Throwable)]()) {
                case (exceptions, (_, Success(_))) => exceptions
                case (exceptions, (requestAddress, Failure(newException))) => (requestAddress, newException) :: exceptions
              }

              if (failures.nonEmpty) {
                failures.foreach {
                  case (failedEmail, exception) =>
                    log.error(s"Failed to send access request email to $failedEmail", exception)
                }
                complete(StatusCodes.InternalServerError)
              } else complete(StatusCodes.OK)
            }
          }
        },
        (post & path("alerts")) {
          authByRole(CreateAlerts) {
            import uk.gov.homeoffice.drt.alerts.MultiPortAlertJsonSupport._
            headerValueByName("X-Auth-Roles") { rolesStr =>
              headerValueByName("X-Auth-Email") { email =>
                entity(as[MultiPortAlert]) {
                  multiPortAlert =>
                    {
                      val user = User.fromRoles(email, rolesStr)
                      val futureResponses = MultiPortAlertClient.saveAlertsForPorts(portCodes, multiPortAlert, user)
                      complete(Future.sequence(futureResponses).map(_ => StatusCodes.Created))
                    }
                }
              }
            }
          }
        },
        (post & path("red-list-updates")) {
          authByRole(RedListsEdit) {
            import uk.gov.homeoffice.drt.redlist.RedListJsonFormats._
            headerValueByName("X-Auth-Roles") { rolesStr =>
              headerValueByName("X-Auth-Email") { email =>
                entity(as[SetRedListUpdate]) {
                  setRedListUpdate =>
                    println(s"Received a SetRedListUpdate to post to ports")
                    println(s"$setRedListUpdate")
                    complete(Future(StatusCodes.OK))
                }
              }
            }
          }
        },
        (get & path("alerts")) {
          authByRole(CreateAlerts) {
            headerValueByName("X-Auth-Roles") { rolesStr =>
              headerValueByName("X-Auth-Email") { email =>
                import uk.gov.homeoffice.drt.alerts.MultiPortAlertJsonSupport._

                val user = User.fromRoles(email, rolesStr)

                val futurePortAlerts: Seq[Future[(String, List[Alert])]] = user.accessiblePorts.map {
                  case (portCode) =>

                    portCode -> DashboardClient.getWithRoles(
                      s"${Dashboard.drtUriForPortCode(portCode)}/alerts/0",
                      user.roles).flatMap(res => Unmarshal[HttpEntity](res.entity.withContentType(ContentTypes.`application/json`))
                        .to[List[Alert]]
                        .recover {
                          case e: Throwable =>
                            log.error(s"Failed to retrieve alerts for $portCode at ${Dashboard.drtUriForPortCode(portCode)}/alerts/0")
                            List()
                        })
                }
                  .toList
                  .map {
                    case (port, futureAlerts) =>
                      futureAlerts.map(a => port -> a)
                  }

                complete(Future.sequence(futurePortAlerts).map(_.toMap))
              }
            }
          }
        },
        (delete & path("alerts" / Segment)) { port =>
          authByRole(CreateAlerts) {
            headerValueByName("X-Auth-Roles") { rolesStr =>
              headerValueByName("X-Auth-Email") { email =>
                val user = User.fromRoles(email, rolesStr)
                val deleteEndpoint = s"${Dashboard.drtUriForPortCode(port)}/alerts"
                complete(DashboardClient.deleteWithRoles(deleteEndpoint, user.roles).map { res =>
                  res.status
                })
              }
            }
          }
        })
    }
}

