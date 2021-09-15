import {combineReducers, configureStore, getDefaultMiddleware} from "@reduxjs/toolkit";
import {userReducer} from "./userSlice";
import {configReducer} from "./configSlice";
import {redListUpdatesReducer} from "./redListSlice";

export const rootReducer = combineReducers({
  user: userReducer,
  config: configReducer,
  redListUpdates: redListUpdatesReducer
});

export const rootStore = configureStore({
  reducer: rootReducer,
  middleware: [...getDefaultMiddleware()]
});

export type RootState = ReturnType<typeof rootReducer>;

export type AppDispatch = typeof rootStore.dispatch;
