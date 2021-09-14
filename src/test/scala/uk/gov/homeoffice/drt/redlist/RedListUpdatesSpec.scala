package uk.gov.homeoffice.drt.redlist

import org.specs2.mutable.Specification
import spray.json.{DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat}


class RedListUpdatesSpec extends Specification {

  import RedListJsonFormats._
  import spray.json._

  val dateMillis = 1631228400000L

  val setRedListUpdatesJsonStr = s"""{"originalDate":$dateMillis,"redListUpdate":{"effectiveFrom":$dateMillis,"additions":[["France","FRA"]],"removals":["Germany"]}}"""
  "Given a json SetRedListUpdates" >> {
    "I should be able to deserialise it" >> {
      val parsed = setRedListUpdatesJsonStr.parseJson.convertTo[SetRedListUpdate]

      parsed === SetRedListUpdate(dateMillis, RedListUpdate(dateMillis, Map("France" -> "FRA"), List("Germany")))
    }
  }
}
