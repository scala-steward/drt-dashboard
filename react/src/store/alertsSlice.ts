import {createAsyncThunk} from "@reduxjs/toolkit";
import axios from "axios";
import {rootStore} from "./rootReducer";
import ApiClient from "../services/ApiClient";
import {PortAlerts} from "../components/Alerts/ViewAlerts";


export const fetchAlerts = createAsyncThunk(
  'alerts/fetch',
  async (onSuccess: (r: PortAlerts[]) => void) => {
    axios
      .get(ApiClient.alertsEndPoint)
      .then((res) => {
        onSuccess(res.data as PortAlerts[]);
      })
      .catch(reason => {
        console.log('Failed to get red list updates: ' + reason)
        setTimeout(() => rootStore.dispatch(fetchAlerts(onSuccess)), 5000)
      })
  }
)

export type DeleteAlertsRequest = {
  portCode: string
  onSuccess: () => void
}

export const deleteAlertsForPort = createAsyncThunk(
  'alerts/deleteForPort',
  async (request: DeleteAlertsRequest) => {
    axios
      .delete(ApiClient.alertsEndPoint + '/' + request.portCode.toLowerCase())
      .then(() => request.onSuccess())
      .catch(reason => {
        console.log('Failed to get red list updates: ' + reason)
        setTimeout(() => rootStore.dispatch(deleteAlertsForPort({
          portCode: request.portCode,
          onSuccess: request.onSuccess
        })), 5000)
      })
  }
)
