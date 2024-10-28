import {createSlice, PayloadAction} from '@reduxjs/toolkit'
import { FormError } from '../../services/ValidationService'
import { TerminalDataPoint } from './regionalPressureSagas'

interface RegionalPressureState {
  status: string,
  errors: FormError[],
  type: string,
  start: string,
  end: string,
  historicStart: string,
  historicEnd: string,
  interval: string,
  portData: {
    [key: string] : TerminalDataPoint[]
  },
  portTotals: {
    [key: string] : number
  }
  historicPortData: {
    [key: string] : TerminalDataPoint[]
  },
  historicPortTotals: {
    [key: string] : number
  }
}

type SetStatePayload = {
  status: string,
  type: string,
  start: string,
  end: string,
  interval: string,
  portData: {
    [key: string] : TerminalDataPoint[]
  },
  portTotals: {
    [key: string] : number
  },
  historicPortData: {
    [key: string] : TerminalDataPoint[]
  },
  historicPortTotals: {
    [key: string] : number
  },
  historicStart: string,
  historicEnd: string,
}

const regionalPressureSlice = createSlice({
  name: 'regionalPressure',
  initialState: {
    status: '',
    portData: {},
    portTotals: {},
    historicPortData: {},
    historicPortTotals: {},
    errors: [],
    type: "single",
    start: new Date().toString(),
    end: new Date().toString(),
    historicStart: new Date().toString(),
    historicEnd: new Date().toString(),
    interval: "daily",
  } as RegionalPressureState,
  reducers: {
    setStatus: (state: RegionalPressureState, action: PayloadAction<string>) => {
      state.status = action.payload;
    },
    setRegionalDashboardState: (state: RegionalPressureState, action: PayloadAction<SetStatePayload>) => {
      state.portData = {...action.payload.portData}
      state.portTotals = {...action.payload.portTotals}
      state.historicPortData = {...action.payload.historicPortData}
      state.historicPortTotals = {...action.payload.historicPortTotals}
      state.type = action.payload.type;
      state.start = action.payload.start;
      state.end = action.payload.end;
      state.interval = action.payload.interval;
      state.status = action.payload.status;
      state.historicStart = action.payload.historicStart;
      state.historicEnd = action.payload.historicEnd;
    },
  }
});

export const {
  setStatus,
  setRegionalDashboardState
} = regionalPressureSlice.actions;

export default regionalPressureSlice.reducer;
