package uk.gov.homeoffice.drt.redlist

import org.specs2.mutable.Specification
import spray.json.DefaultJsonProtocol.{LongJsonFormat, listFormat, mapFormat, seqFormat}


class RedListUpdatesSpec extends Specification {

  import RedListJsonFormats._
  import spray.json._

  val dateMillis = 1631228400000L

  val setRedListUpdatesJsonStr =
    s"""{
       |  "originalDate":$dateMillis,
       |  "redListUpdate":{
       |    "effectiveFrom":$dateMillis,
       |    "additions":[
       |      ["France","FRA"]
       |    ],
       |    "removals": [
       |      "Germany"
       |    ]
       |  }
       |}""".stripMargin

  "Given a json SetRedListUpdates" >> {
    "I should be able to deserialise it" >> {
      val parsed = setRedListUpdatesJsonStr.parseJson.convertTo[SetRedListUpdate]

      parsed === SetRedListUpdate(dateMillis, RedListUpdate(dateMillis, Map("France" -> "FRA"), List("Germany")))
    }
  }

  "Given a json RedListUpdates" >> {
    "I should be able to deserialise it" >> {
      val parsed = redListUpdatesJsonStr.parseJson.convertTo[List[RedListUpdate]].map(r => (r.effectiveFrom, r)).toMap
      parsed === Map(1613347200000L -> RedListUpdate(additions = Map("Angola" -> "AGO", "Argentina" -> "ARG"), removals = List(), effectiveFrom = 1613347200000L))
    }
  }

  def redListUpdatesJsonStr: String =
    """[
      |		{
      |			"additions":[
      |				["Angola","AGO"],
      |				["Argentina","ARG"]
      |			],
      |			"effectiveFrom":1613347200000,
      |			"removals":[]
      |		}
      |]""".stripMargin
}
