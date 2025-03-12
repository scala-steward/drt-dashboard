import {createSlice, PayloadAction} from '@reduxjs/toolkit'
import {FormError} from '../../services/ValidationService'
import {TerminalDataPoint} from './regionalPressureSagas'
import moment, {Moment} from "moment/moment";

export const getHistoricDateByDay: (date: Moment) => Moment = (date: Moment) => {
  return moment(date)
    .subtract(1, 'year')
    .isoWeek(date.isoWeek())
    .isoWeekday(date.isoWeekday())
}

interface RegionalPressureState {
  status: string,
  errors: FormError[],
  singleOrRange: 'single' | 'range',
  comparisonType: 'previousYear' | 'custom',
  forecastStart: string,
  forecastEnd: string,
  historicStart: string,
  historicEnd: string,
  interval: 'hour' | 'day',
  forecastHourlyPaxByPort: {
    [key: string] : TerminalDataPoint[]
  },
  forecastTotalPaxByPort: {
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
  singleOrRange: 'single' | 'range',
  comparisonType: 'previousYear' | 'custom',
  interval: 'day' | 'hour',
  forecastStart: string,
  forecastEnd: string,
  forecastHourlyPaxByPort: {
    [key: string] : TerminalDataPoint[]
  },
  forecastTotalPaxByPort: {
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

const historicStart = () => getHistoricDateByDay(moment())

const regionalPressureSlice = createSlice({
  name: 'regionalPressure',
  initialState: {
    status: '',
    forecastHourlyPaxByPort: {},
    forecastTotalPaxByPort: {},
    historicHourlyPaxByPort: {},
    historicTotalPaxByPort: {},
    errors: [],
    singleOrRange: 'single',
    comparisonType: 'previousYear',
    interval: 'day',
    forecastStart: new Date().toString(),
    forecastEnd: new Date().toString(),
    historicStart: historicStart().format('YYYY-MM-DD'),
    historicEnd: historicStart().format('YYYY-MM-DD'),
  } as RegionalPressureState,
  reducers: {
    setStatus: (state: RegionalPressureState, action: PayloadAction<string>) => {
      state.status = action.payload;
    },
    setRegionalDashboardState: (state: RegionalPressureState, action: PayloadAction<SetStatePayload>) => {
      state.forecastHourlyPaxByPort = {...action.payload.forecastHourlyPaxByPort}
      state.forecastTotalPaxByPort = {...action.payload.forecastTotalPaxByPort}
      state.historicHourlyPaxByPort = {...action.payload.historicHourlyPaxByPort}
      state.historicTotalPaxByPort = {...action.payload.historicTotalPaxByPort}
      state.interval = action.payload.interval;
      state.status = action.payload.status;
      state.singleOrRange = action.payload.singleOrRange;
      state.comparisonType = action.payload.comparisonType;
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
