import {createAsyncThunk, createSlice, PayloadAction} from "@reduxjs/toolkit";
import axios from "axios";
import {rootStore} from "./rootReducer";
import ApiClient from "../services/ApiClient";
import {User, UserProfile} from "../model/User";


export const fetchUserProfile = createAsyncThunk(
  'user/profile',
  async () => {
    axios
      .get(ApiClient.userEndPoint)
      .then((res) =>
        rootStore.dispatch(userSlice.actions.signIn(res.data as UserProfile)))
      .catch(reason =>
        console.log('Failed to get user profile: ' + reason))
      .finally(() =>
        setTimeout(() => rootStore.dispatch(fetchUserProfile()), 5000))
  }
)

export const userSlice = createSlice({
    name: 'userData',
    initialState: {kind: "PendingUser"} as User,
    reducers: {
      signIn(state: User, action: PayloadAction<UserProfile>) {
        return {kind: "SignedInUser", profile: action.payload}
      },
      signOut() {
        console.log('Setting user SignedOutUser')
        return {kind: "SignedOutUser"}
      }
    }
  }
)

export const userReducer = userSlice.reducer
