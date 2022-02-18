package uk.gov.homeoffice.drt.redlist

import spray.json.{ DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat, enrichAny }

trait RedListJsonFormats extends DefaultJsonProtocol {
  implicit object redListUpdateJsonFormat extends RootJsonFormat[RedListUpdate] {
    override def write(obj: RedListUpdate): JsValue = JsObject(Map(
      "effectiveFrom" -> JsNumber(obj.effectiveFrom),
      "additions" -> JsArray(obj.additions.map(a => JsArray(JsString(a._1), JsString(a._2))).toVector),
      "removals" -> JsArray(obj.removals.map(r => JsString(r)).toVector)))

    override def read(json: JsValue): RedListUpdate = json match {
      case JsObject(fields) =>
        val maybeStuff = for {
          effectiveFrom <- fields.get("effectiveFrom").collect { case JsNumber(value) => value.toLong }
          additions <- fields.get("additions").collect {
            case JsArray(things) =>
              val namesWithCodes = things.collect {
                case JsArray(nameAndCode) =>
                  nameAndCode.toList match {
                    case JsString(n) :: JsString(c) :: tail => (n, c)
                    case _ => throw new Exception("Didn't find country name and code for RedListUpdate additions")
                  }
                case unexpected => throw new Exception(s"Expected to find JsArray, but got ${unexpected.getClass}")
              }.toMap
              namesWithCodes
            case unexpected => throw new Exception(s"Expected to find JsArray, but got ${unexpected.getClass}")
          }
          removals <- fields.get("removals").collect { case JsArray(stuff) => stuff.map(_.convertTo[String]).toList }
        } yield RedListUpdate(effectiveFrom, additions, removals)
        maybeStuff.getOrElse(throw new Exception("Failed to deserialise RedListUpdate json"))
    }
  }

  implicit val setRedListUpdatesJsonFormat: RootJsonFormat[SetRedListUpdate] = jsonFormat2(SetRedListUpdate.apply)

  implicit object redListUpdatesJsonFormat extends RootJsonFormat[RedListUpdates] {
    override def write(obj: RedListUpdates): JsValue = JsArray(obj.updates.values.map(_.toJson).toVector)

    override def read(json: JsValue): RedListUpdates = json match {
      case JsObject(fields) => fields.get("updates") match {
        case Some(JsObject(updates)) =>
          val redListUpdates = updates.map {
            case (effectiveFrom, redListUpdateJson) => (effectiveFrom.toLong, redListUpdateJson.convertTo[RedListUpdate])
          }
          RedListUpdates(redListUpdates)
      }
    }
  }
}
