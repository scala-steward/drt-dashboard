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
  interval: 'hour' | 'day',
  currentHourlyPaxByPort: {
    [key: string] : TerminalDataPoint[]
  },
  currentTotalPaxByPort: {
    [key: string] : number
  }
  historicHourlyPaxByPort: {
    [key: string] : TerminalDataPoint[]
  },
  historicTotalPaxByPort: {
    [key: string] : number
  }
}

type SetStatePayload = {
  status: string,
  type: string,
  start: string,
  end: string,
  interval: 'hour' | 'day',
  currentHourlyPaxByPort: {
    [key: string] : TerminalDataPoint[]
  },
  currentTotalPaxByPort: {
    [key: string] : number
  },
  historicHourlyPaxByPort: {
    [key: string] : TerminalDataPoint[]
  },
  historicTotalPaxByPort: {
    [key: string] : number
  },
  historicStart: string,
  historicEnd: string,
}

const regionalPressureSlice = createSlice({
  name: 'regionalPressure',
  initialState: {
    status: '',
    currentHourlyPaxByPort: {},
    currentTotalPaxByPort: {},
    historicHourlyPaxByPort: {},
    historicTotalPaxByPort: {},
    errors: [],
    type: 'single',
    start: new Date().toString(),
    end: new Date().toString(),
    historicStart: new Date().toString(),
    historicEnd: new Date().toString(),
    interval: 'day',
  } as RegionalPressureState,
  reducers: {
    setStatus: (state: RegionalPressureState, action: PayloadAction<string>) => {
      state.status = action.payload;
    },
    setRegionalDashboardState: (state: RegionalPressureState, action: PayloadAction<SetStatePayload>) => {
      state.currentHourlyPaxByPort = {...action.payload.currentHourlyPaxByPort}
      state.currentTotalPaxByPort = {...action.payload.currentTotalPaxByPort}
      state.historicHourlyPaxByPort = {...action.payload.historicHourlyPaxByPort}
      state.historicTotalPaxByPort = {...action.payload.historicTotalPaxByPort}
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
