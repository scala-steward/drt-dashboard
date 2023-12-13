import {createSlice, PayloadAction} from '@reduxjs/toolkit'

interface DownloadManagerState {
  status: string
  createdAt: string
  downloadLink: string
}

const downloadManagerSlice = createSlice({
  name: 'downloadManager',
  initialState: {
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
  }
});

export const {
  setStatus,
  setCreatedAt,
  setDownloadLink,
} = downloadManagerSlice.actions;

export default downloadManagerSlice.reducer;
