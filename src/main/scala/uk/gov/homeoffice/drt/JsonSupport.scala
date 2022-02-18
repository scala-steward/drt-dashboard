package uk.gov.homeoffice.drt

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, RootJsonFormat }
import uk.gov.homeoffice.drt.routes.{ FeedStatus, FlightData }

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val flightDataFormat: RootJsonFormat[FlightData] = jsonFormat7(FlightData)
  implicit val feedStatusFormat: RootJsonFormat[FeedStatus] = jsonFormat3(FeedStatus)
}
