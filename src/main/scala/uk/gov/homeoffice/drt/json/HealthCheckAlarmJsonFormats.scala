package uk.gov.homeoffice.drt.json

import spray.json.{DefaultJsonProtocol, JsArray, JsObject, JsString, JsValue, RootJsonFormat, enrichAny}
import uk.gov.homeoffice.drt.ports.PortCode

trait HealthCheckAlarmJsonFormats extends DefaultJsonProtocol {
  implicit object portCodeFormat extends RootJsonFormat[PortCode] {
    override def read(json: JsValue): PortCode = json match {
      case JsString(portCode) => PortCode(portCode)
    }

    override def write(obj: PortCode): JsValue = JsString(obj.iata)
  }

  implicit object alarmStatusFormat extends RootJsonFormat[Map[PortCode, Map[String, Boolean]]] {
    override def read(json: JsValue): Map[PortCode, Map[String, Boolean]] = throw new UnsupportedOperationException("Not implemented")

    override def write(obj: Map[PortCode, Map[String, Boolean]]): JsValue = JsArray(
      obj.map {
        case (portCode, alarmStatuses) =>
          JsObject(Map(
            "port" -> portCode.toJson,
            "alarms" -> JsArray(alarmStatuses.map {
              case (checkName, isActive) => JsObject(Map(
                "name" -> checkName.toJson,
                "isActive" -> isActive.toJson,
              ))
            }.toVector)
          ))
      }.toVector
    )
  }
}
