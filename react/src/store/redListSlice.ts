import {createAsyncThunk} from "@reduxjs/toolkit";
import axios from "axios";
import {rootStore} from "./rootReducer";
import ApiClient from "../services/ApiClient";
import {RedListUpdate} from "../components/redlisteditor/model";
import {SetRedListUpdates} from "../components/RedListEditor";


export const fetchRedListUpdates = createAsyncThunk(
  'redListUpdates/fetch',
  async (onSuccess: (r: RedListUpdate[]) => void) => {
    axios
      .get(ApiClient.redListUpdates)
      .then((res) => {
          onSuccess(res.data as RedListUpdate[]);
        })
      .catch(reason => {
        console.log('Failed to get red list updates: ' + reason)
        setTimeout(() => rootStore.dispatch(fetchRedListUpdates(onSuccess)), 5000)
      })
  }
)

export type RequestSetRedListUpdates = {
  updates: SetRedListUpdates
  onSuccess: () => void
  onFailure: () => void
}

export type RequestDeleteRedListUpdates = {
  dateToDelete: number
  onSuccess: () => void
  onFailure: () => void
}

export const saveRedListUpdates = createAsyncThunk(
  'redListUpdates/save',
  async (request: RequestSetRedListUpdates) => {
    axios
      .post(ApiClient.redListUpdates, request.updates)
      .then(() => request.onSuccess())
      .catch(reason => {
        console.log('Failed to save red list updates: ' + reason)
        request.onFailure()
      })
  }
)

export const deleteRedListUpdates = createAsyncThunk(
  'redListUpdates/delete',
  async (request: RequestDeleteRedListUpdates) => {
    axios
      .delete(ApiClient.redListUpdates + '/' + request.dateToDelete)
      .then(() => request.onSuccess())
      .catch(reason => {
        console.log('Failed to delete red list updates: ' + reason)
        request.onFailure()
      })
  }
)
