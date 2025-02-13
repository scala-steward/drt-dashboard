import {createSlice, PayloadAction} from '@reduxjs/toolkit'
import { FormError } from '../../services/ValidationService'
import { TerminalDataPoint } from './regionalPressureSagas'

interface RegionalPressureState {
  status: string,
  errors: FormError[],
  singleOrRange: 'single' | 'range',
  comparisonType: 'previousYear' | 'custom',
  interval: string,
  forecastStart: string,
  forecastEnd: string,
  historicStart: string,
  historicEnd: string,
  forecastData: {
    [key: string] : TerminalDataPoint[]
  },
  forecastTotals: {
    [key: string] : number
  }
  historicData: {
    [key: string] : TerminalDataPoint[]
  },
  historicTotals: {
    [key: string] : number
  }
}

type SetStatePayload = {
  status: string,
  singleOrRange: 'single' | 'range',
  comparisonType: 'previousYear' | 'custom',
  interval: string,
  forecastStart: string,
  forecastEnd: string,
  forecastData: {
    [key: string] : TerminalDataPoint[]
  },
  forecastTotals: {
    [key: string] : number
  },
  historicData: {
    [key: string] : TerminalDataPoint[]
  },
  historicTotals: {
    [key: string] : number
  },
  historicStart: string,
  historicEnd: string,
}

const regionalPressureSlice = createSlice({
  name: 'regionalPressure',
  initialState: {
    status: '',
    errors: [],
    singleOrRange: 'single',
    comparisonType: 'previousYear',
    interval: 'daily',
    forecastData: {},
    forecastTotals: {},
    historicData: {},
    historicTotals: {},
    forecastStart: new Date().toString(),
    forecastEnd: new Date().toString(),
    historicStart: new Date().toString(),
    historicEnd: new Date().toString(),
  } as RegionalPressureState,
  reducers: {
    setStatus: (state: RegionalPressureState, action: PayloadAction<string>) => {
      state.status = action.payload;
    },
    setRegionalDashboardState: (state: RegionalPressureState, action: PayloadAction<SetStatePayload>) => {
      state.status = action.payload.status;
      state.singleOrRange = action.payload.singleOrRange;
      state.comparisonType = action.payload.comparisonType;
      state.interval = action.payload.interval;
      state.forecastData = {...action.payload.forecastData}
      state.forecastTotals = {...action.payload.forecastTotals}
      state.historicData = {...action.payload.historicData}
      state.historicTotals = {...action.payload.historicTotals}
      state.forecastStart = action.payload.forecastStart;
      state.forecastEnd = action.payload.forecastEnd;
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
