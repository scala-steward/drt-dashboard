import {createAsyncThunk} from "@reduxjs/toolkit";
import axios from "axios";
import {rootStore} from "./rootReducer";
import ApiClient from "../services/ApiClient";
import {HealthCheckPause} from "../components/healthcheckpauseseditor/model";
import {SetHealthCheckPauses} from "../components/HealthCheckPausesEditor";


export const fetchHealthCheckPauses = createAsyncThunk(
  'healthCheckPauses/fetch',
  async (onSuccess: (r: HealthCheckPause[]) => void) => {
    axios
      .get(ApiClient.healthCheckPauses)
      .then((res) => {
        onSuccess(res.data as HealthCheckPause[]);
      })
      .catch(reason => {
        console.log('Failed to get red list updates: ' + reason)
        setTimeout(() => rootStore.dispatch(fetchHealthCheckPauses(onSuccess)), 5000)
      })
  }
)

export type RequestSetHealthCheckPauses = {
  updates: SetHealthCheckPauses
  onSuccess: () => void
  onFailure: () => void
}

export type RequestDeleteHealthCheckPauses = {
  dateToDelete: number
  onSuccess: () => void
  onFailure: () => void
}

export const saveHealthCheckPauses = createAsyncThunk(
  'healthCheckPauses/save',
  async (request: RequestSetHealthCheckPauses) => {
    axios
      .post(ApiClient.healthCheckPauses, request.updates)
      .then(() => request.onSuccess())
      .catch(reason => {
        console.log('Failed to save red list updates: ' + reason)
        request.onFailure()
      })
  }
)

export const deleteHealthCheckPauses = createAsyncThunk(
  'healthCheckPauses/delete',
  async (request: RequestDeleteHealthCheckPauses) => {
    axios
      .delete(ApiClient.healthCheckPauses + '/' + request.dateToDelete)
      .then(() => request.onSuccess())
      .catch(reason => {
        console.log('Failed to delete red list updates: ' + reason)
        request.onFailure()
      })
  }
)
