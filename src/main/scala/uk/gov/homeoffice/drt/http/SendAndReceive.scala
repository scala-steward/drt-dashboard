package uk.gov.homeoffice.drt.http

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse }

import scala.concurrent.Future

trait WithSendAndReceive {
  type SendReceive = HttpRequest => Future[HttpResponse]
  def sendAndReceive: SendReceive
}

trait ProdSendAndReceive extends WithSendAndReceive {
  implicit val system: ActorSystem[Nothing]

  override def sendAndReceive: SendReceive = request => Http()(system).singleRequest(request)
}

