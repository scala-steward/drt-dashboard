import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";
import axios from "axios";
import {rootStore} from "./rootReducer";
import ApiClient from "../services/ApiClient";
import {User, UserProfile} from "../model/User";
import {RedListUpdates} from "../components/redlisteditor/model";
import {SetRedListUpdates} from "../components/RedListEditor";


export const fetchRedListUpdates = createAsyncThunk(
  'user/profile',
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

export const saveRedListUpdates = createAsyncThunk(
  'user/profile',
  async (request: RequestSetRedListUpdates) => {
    axios
      .post(ApiClient.redListUpdates, request.updates)
      .then(res => request.onSuccess())
      .catch(reason => {
        console.log('Failed to save red list updates: ' + reason)
        request.onFailure()
      })
  }
)

export const redListUpdatesSlice = createSlice({
    name: 'redListData',
    initialState: {updates: new Map()} as RedListUpdates,
    reducers: {
      setRedListUpdates(state: RedListUpdates, action: PayloadAction<RedListUpdates>) {
        return action.payload
      }
    }
  }
)

export const redListUpdatesReducer = redListUpdatesSlice.reducer
