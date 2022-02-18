package uk.gov.homeoffice.drt.redlist

import org.specs2.mutable.Specification

class RedListUpdatesSpec extends Specification with RedListJsonFormats {

  import spray.json._

  val dateMillis = 1631228400000L

  val setRedListUpdatesJsonStr: String =
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

  "Given a long json RedListUpdates" >> {
    "I should be able to deserialise it and serialise it again" >> {
      val parsed = redListUpdatesJsonStr.parseJson.convertTo[List[RedListUpdate]].map(r => (r.effectiveFrom, r)).toMap
      val serialised = RedListUpdates(parsed).toJson
      serialised.compactPrint === redListUpdatesJsonStr
    }
  }

  def redListUpdatesJsonStr: String =
    """[{"effectiveFrom":1613347200000,"additions":[["Angola","AGO"],["Argentina","ARG"]],"removals":[]}]""".stripMargin

  def longUpdatesJsonStr: String =
    """[{"effectiveFrom":0,"additions":[],"removals":[]},{"effectiveFrom":1613347200000,"additions":[["Eswatini","SWZ"],["Angola","AGO"],["Venezuela","VEN"],["Cape Verde","CPV"],["Rwanda","RWA"],["Namibia","NAM"],["Argentina","ARG"],["Peru","PER"],["Mauritius","MUS"],["Suriname","SUR"],["French Guiana","GUF"],["Paraguay","PRY"],["Seychelles","SYC"],["South Africa","ZAF"],["Brazil","BRA"],["Ecuador","ECU"],["Zambia","ZMB"],["Botswana","BWA"],["Burundi","BDI"],["Uruguay","URY"],["Panama","PAN"],["Malawi","MWI"],["Zimbabwe","ZWE"],["Bolivia","BOL"],["Chile","CHL"],["Mozambique","MOZ"],["Tanzania","TZA"],["United Arab Emirates","ARE"],["Lesotho","LSO"],["Guyana","GUY"],["Colombia","COL"],["Portugal","PRT"],["Congo (Kinshasa)","COD"]],"removals":[]},{"effectiveFrom":1616112000000,"additions":[["Ethiopia","ETH"],["Oman","OMN"],["Qatar","QAT"],["Somalia","SOM"]],"removals":["Portugal","Mauritius"]},{"effectiveFrom":1617922800000,"additions":[["Philippines","PHL"],["Pakistan","PAK"],["Kenya","KEN"],["Bangladesh","BGD"]],"removals":[]},{"effectiveFrom":1619132400000,"additions":[["India","IND"]],"removals":[]},{"effectiveFrom":1620774000000,"additions":[["Turkey","TUR"],["Maldives","MDV"],["Nepal","NPL"]],"removals":[]},{"effectiveFrom":1623106800000,"additions":[["Trinidad and Tobago","TTO"],["Afghanistan","AFG"],["Sri Lanka","LKA"],["Egypt","EGY"],["Costa Rica","CRI"],["Bahrain","BHR"],["Sudan","SDN"]],"removals":[]},{"effectiveFrom":1625007600000,"additions":[["Haiti","HTI"],["Eritrea","ERI"],["Mongolia","MNG"],["Tunisia","TUN"],["Uganda","UGA"],["Dominican Republic","DOM"]],"removals":[]},{"effectiveFrom":1626649200000,"additions":[["Cuba","CUB"],["Indonesia","IDN"],["Myanmar","MMR"],["Sierra Leone","SLE"]],"removals":[]},{"effectiveFrom":1628377200000,"additions":[["Georgia","GEO"],["Mayotte","MYT"],["Mexico","MEX"],["Reunion","REU"]],"removals":["Bahrain","India","Qatar","United Arab Emirates"]},{"effectiveFrom":1630278000000,"additions":[["Thailand","THA"],["Montenegro","MNE"],["Germany","GER"]],"removals":[]},{"effectiveFrom":1631228400000,"additions":[["France","FRA"],["Italy","ITA"],["Canada","CAN"]],"removals":[]}]"""
}
