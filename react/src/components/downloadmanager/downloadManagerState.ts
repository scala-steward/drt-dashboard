import {createSlice, PayloadAction} from '@reduxjs/toolkit'
import { FormError } from '../../services/ValidationService'

interface DownloadManagerState {
  errors: FormError[],
  status: string
  createdAt: string
  downloadLink: string
}

const downloadManagerSlice = createSlice({
  name: 'downloadManager',
  initialState: {
    errors: [],
    status: "",
    createdAt: "",
    downloadLink: "",
  } as DownloadManagerState,
  reducers: {
    setStatus: (state: DownloadManagerState, action: PayloadAction<string>) => {
      state.status = action.payload;
    },
    setCreatedAt: (state: DownloadManagerState, action: PayloadAction<string>) => {
      state.createdAt = action.payload;
    },
    setDownloadLink: (state: DownloadManagerState, action: PayloadAction<string>) => {
      state.downloadLink = action.payload;
    },
    addErrors: (state: DownloadManagerState, action: PayloadAction<FormError[]>) => {
      state.errors = [
        ...state.errors,
        ...action.payload
      ];
    },
    clearErrors: (state: DownloadManagerState) => {
      state.errors = [];
    },
  }
});

export const {
  setStatus,
  setCreatedAt,
  setDownloadLink,
  addErrors,
  clearErrors,
} = downloadManagerSlice.actions;

export default downloadManagerSlice.reducer;
