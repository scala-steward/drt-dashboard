import { createSlice } from '@reduxjs/toolkit'

const downloadManagerSlice = createSlice({
  name: 'downloadManager',
  initialState: {
    status: "",
    createdAt: "",
  },
  reducers: {
    setStatus: (state, action) => {
      state.status = action.payload;
    },
    setCreatedAt: (state, action) => {
      state.createdAt = action.payload;
    },
  }
});

export const { 
  setStatus, 
  setCreatedAt,
} = downloadManagerSlice.actions;

export default downloadManagerSlice.reducer;
