package uk.gov.homeoffice.drt.redlist

import spray.json.{ DefaultJsonProtocol, JsArray, JsNumber, JsObject, JsString, JsValue, RootJsonFormat }

object RedListJsonFormats {

  import DefaultJsonProtocol._

  implicit object redListUpdateJsonFormat extends RootJsonFormat[RedListUpdate] {
    override def write(obj: RedListUpdate): JsValue = throw new Exception("Serialisation not implemented yet")

    override def read(json: JsValue): RedListUpdate = json match {
      case JsObject(fields) =>
        val maybeStuff = for {
          effectiveFrom <- fields.get("effectiveFrom").collect { case JsNumber(value) => value.toLong }
          additions <- fields.get("additions").collect {
            case JsArray(things) =>
              val namesWithCodes = things.collect {
                case JsArray(nameAndCode) =>
                  nameAndCode.toList match {
                    case JsString(n) :: JsString(c) :: Nil => (n, c)
                  }
              }.toMap
              namesWithCodes
          }
          removals <- fields.get("removals").collect { case JsArray(stuff) => stuff.map(_.convertTo[String]).toList }
        } yield RedListUpdate(effectiveFrom, additions, removals)
        maybeStuff.getOrElse(throw new Exception("Failed to deserialise RedListUpdate json"))
    }
  }

  implicit val setRedListUpdatesJsonFormat: RootJsonFormat[SetRedListUpdate] = jsonFormat2(SetRedListUpdate.apply)

}
