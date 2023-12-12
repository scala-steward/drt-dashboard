import axios from "axios";
import ApiClient from "../services/ApiClient";
import {PortAlerts} from "../components/alerts/ViewAlerts";


export const fetchAlerts = (onSuccess: (r: PortAlerts[]) => void) => {
  axios
    .get(ApiClient.alertsEndPoint)
    .then((res) => {
      onSuccess(res.data as PortAlerts[]);
    })
    .catch(reason => {
      console.log('Failed to get alert updates: ' + reason)
      setTimeout(() => fetchAlerts(onSuccess), 5000)
    })
}

export type DeleteAlertsRequest = {
  portCode: string
  onSuccess: () => void
}

export const deleteAlertsForPort = (request: DeleteAlertsRequest) => {
  axios
    .delete(ApiClient.alertsEndPoint + '/' + request.portCode.toLowerCase())
    .then(() => request.onSuccess())
    .catch(reason => {
      console.log('Failed to get alert updates: ' + reason)
      setTimeout(() => deleteAlertsForPort({
        portCode: request.portCode,
        onSuccess: request.onSuccess
      }), 5000)
    })
}
