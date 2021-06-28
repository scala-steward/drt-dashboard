package uk.gov.homeoffice.drt

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import uk.gov.homeoffice.drt.routes.{ FeedStatus, FlightData }

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit val flightDataFormat = jsonFormat4(FlightData)

  implicit val feedStatusFormat = jsonFormat3(FeedStatus)

}
