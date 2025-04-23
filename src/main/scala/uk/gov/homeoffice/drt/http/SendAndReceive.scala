package uk.gov.homeoffice.drt.http

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.{ HttpRequest, HttpResponse }

import scala.concurrent.Future

trait WithSendAndReceive {
  type SendReceive = HttpRequest => Future[HttpResponse]
  def sendAndReceive: SendReceive
}

trait ProdSendAndReceive extends WithSendAndReceive {
  implicit val system: ActorSystem[Nothing]

  override def sendAndReceive: SendReceive = request => Http()(system).singleRequest(request)
}

