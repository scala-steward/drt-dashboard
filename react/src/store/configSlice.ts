import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";
import axios from "axios";
import {rootStore} from "./rootReducer";
import ApiClient from "../services/ApiClient";
import {Config, ConfigValues} from "../model/Config";


export const fetchConfig = createAsyncThunk(
  'config/values',
  async () => {
    axios
      .get(ApiClient.userEndPoint)
      .then((res) =>
        rootStore.dispatch(configSlice.actions.setConfig(res.data as ConfigValues)))
      .catch(reason => {
        console.log('Failed to get config. Will retry. ' + reason)
        setTimeout(() => rootStore.dispatch(fetchConfig()), 5000)
      })
  }
)

export const configSlice = createSlice({
    name: 'configData',
    initialState: {kind: "PendingConfig"} as Config,
    reducers: {
      setConfig(state: Config, action: PayloadAction<ConfigValues>) {
        return {kind: "LoadedConfig", values: action.payload}
      }
    }
  }
)

export const configReducer = configSlice.reducer
