package uk.gov.homeoffice.drt.notifications

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.stream.Materializer
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.specs2.mutable.Specification
import uk.gov.homeoffice.drt.HttpClient

import scala.concurrent.{ExecutionContextExecutor, Future}

class SlackClientSpec extends Specification {
  val testKit: ActorTestKit = ActorTestKit()
  implicit val sys: ActorSystem[Nothing] = testKit.system
  implicit val ec: ExecutionContextExecutor = sys.executionContext
  implicit val mat: Materializer = Materializer(sys)

  val mockHttpClient = Mockito.mock(classOf[HttpClient])
  val webhookUrl = "https://test.com"
  "SlackClient notify" >> {
    "should send a message to the slack webhook" >> {
      val message = "Test message"
      val payload = s"""{"text": "$message"}"""
      val entity = HttpEntity(ContentTypes.`application/json`, payload)

      val httpRequest = HttpRequest(method = HttpMethods.POST, uri = webhookUrl, entity = entity)
      when(mockHttpClient.send(httpRequest)).thenReturn(Future.successful(HttpResponse(entity = "ok")))
      val slackClient = SlackClientImpl(mockHttpClient, webhookUrl)
      slackClient.notify(message)
      Mockito.verify(mockHttpClient, Mockito.times(1)).send(httpRequest)
      ok
    }
  }
}
