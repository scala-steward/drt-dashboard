import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";
import axios from "axios";
import {rootStore} from "./rootReducer";
import ApiClient from "../services/ApiClient";
import {RedListUpdates} from "../components/redlisteditor/model";
import {SetRedListUpdates} from "../components/RedListEditor";


export const fetchRedListUpdates = createAsyncThunk(
  'redListUpdates/fetch',
  async () => {
    axios
      .get(ApiClient.redListUpdates)
      .then((res) =>
        rootStore.dispatch(redListUpdatesSlice.actions.setRedListUpdates(res.data as RedListUpdates)))
      .catch(reason =>
        console.log('Failed to get user profile: ' + reason))
      .finally(() =>
        setTimeout(() => rootStore.dispatch(fetchRedListUpdates()), 5000))
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

export const redListUpdatesSlice = createSlice({
    name: 'redListData',
    initialState: {updates: []} as RedListUpdates,
    reducers: {
      setRedListUpdates(state: RedListUpdates, action: PayloadAction<RedListUpdates>) {
        return action.payload
      }
    }
  }
)

export const redListUpdatesReducer = redListUpdatesSlice.reducer
