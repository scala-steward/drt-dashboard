import {call, put, takeEvery} from 'redux-saga/effects'
import {setRegionalDashboardState, setStatus} from './regionalPressureState'
import StubService from '../../services/stub-service'
import moment, {Moment} from 'moment'
import ApiClient from '../../services/ApiClient'
import axios from 'axios'
import {download, generateCsv} from "export-to-csv"

export type RequestPaxTotalsType = {
  type: "REQUEST_PAX_TOTALS",
  singleOrRange: 'single' | 'range',
  comparisonType: 'previousYear' | 'custom',
  userPorts: string[],
  availablePorts: string[],
  forecastStart: string,
  forecastEnd: string,
  isExport: boolean,
  historicStart: string,
  historicEnd: string,
}

export type QueueCount = {
  queueName: string,
  count: number,
}

export type TerminalDataPoint = {
  date: string,
  hour: number,
  portCode: string,
  queueCounts: QueueCount[],
  regionName: string,
  totalPcpPax: number,
  terminalName?: string,
}


export type ExportableDataPoint = {
  date: string,
  hour: number,
  portCode: string,
  regionName: string,
  totalPcpPax: number,
  terminalName?: string,
  EEA?: number,
  nonEEA?: number,
  eGates?: number,
}

export type PortsObject = {
  [key: string]: TerminalDataPoint[]
}

export type PortTotals = {
  [key: string]: number
}

type APIResponse = {
  data: TerminalDataPoint[]
}

export const requestPaxTotals = (
  userPorts: string[],
  availablePorts: string[],
  singleOrRange: 'single' | 'range',
  comparisonType: 'previousYear' | 'custom',
  startDate: string,
  endDate: string,
  isExport: boolean,
  historicStart: string,
  historicEnd: string,
): RequestPaxTotalsType => {
  return {
    "type": "REQUEST_PAX_TOTALS",
    singleOrRange,
    comparisonType,
    userPorts,
    availablePorts,
    forecastStart: startDate,
    forecastEnd: endDate,
    isExport,
    historicStart,
    historicEnd,
  }
}

export function getHistoricDateByDay(date: Moment): Moment {
  return moment(date)
    .subtract(1, 'year')
    .isoWeek(date.isoWeek())
    .isoWeekday(date.isoWeekday())
}

const createExportableDatapoints = (datapoints: TerminalDataPoint[]): ExportableDataPoint[] => {
  let flattenedCurrent: ExportableDataPoint[] = []
  datapoints!.forEach((datapoint) => {
    flattenedCurrent.push({
      date: datapoint.date,
      hour: datapoint.hour,
      portCode: datapoint.portCode,
      regionName: datapoint.regionName,
      totalPcpPax: datapoint.totalPcpPax,
      terminalName: datapoint.terminalName,
      EEA: datapoint.queueCounts[0]?.count || 0,
      eGates: datapoint.queueCounts[1]?.count || 0,
      nonEEA: datapoint.queueCounts[2]?.count || 0,
    })
  })
  return flattenedCurrent
}

export function* handleRequestPaxTotals(action: RequestPaxTotalsType) {
  try {
    yield(put(setStatus('loading')))
    const start = moment(action.forecastStart)
    const end = action.singleOrRange === 'single' ? start : moment(action.forecastEnd).endOf('day')
    const historicStart = moment(action.historicStart)
    const historicEnd = action.singleOrRange === 'single' ? historicStart : moment(action.historicEnd).endOf('day')

    const duration = moment.duration(end.diff(start)).asHours()
    const interval = duration >= 48 ? 'daily' : 'hourly'

    const forecastStart = start.format('YYYY-MM-DD')
    const forecastEnd = end.format('YYYY-MM-DD')
    const fHistoricStart = historicStart.format('YYYY-MM-DD')
    const fHistoricEnd = historicEnd.format('YYYY-MM-DD')

    let current: TerminalDataPoint[]
    let historic: TerminalDataPoint[]
    let currentResponse: APIResponse
    let historicResponse: APIResponse
    if (window.location.hostname.includes('localhost')) {
      //stub all data for local development
      current = StubService.generatePortPaxSeries(forecastStart, forecastEnd, interval, 'region', action.availablePorts)
      historic = StubService.generatePortPaxSeries(fHistoricStart, fHistoricEnd, interval, 'region', action.availablePorts)
    } else {
      currentResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${forecastStart}/${forecastEnd}?granularity=${interval}&port-codes=${action.availablePorts.join()}`)
      historicResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fHistoricStart}/${fHistoricEnd}?granularity=${interval}&port-codes=${action.availablePorts.join()}`)

      current = currentResponse.data
      historic = historicResponse.data

      if (action.availablePorts.includes('LHR')) {
        const LHRT2: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${forecastStart}/${forecastEnd}/T2?granularity=${interval}&port-codes=LHR`)
        const LHRT3: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${forecastStart}/${forecastEnd}/T3?granularity=${interval}&port-codes=LHR`)
        const LHRT4: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${forecastStart}/${forecastEnd}/T4?granularity=${interval}&port-codes=LHR`)
        const LHRT5: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${forecastStart}/${forecastEnd}/T5?granularity=${interval}&port-codes=LHR`)

        const LHRT2Historic: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fHistoricStart}/${fHistoricEnd}/T2?granularity=${interval}&port-codes=LHR`)
        const LHRT3Historic: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fHistoricStart}/${fHistoricEnd}/T3?granularity=${interval}&port-codes=LHR`)
        const LHRT4Historic: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fHistoricStart}/${fHistoricEnd}/T4?granularity=${interval}&port-codes=LHR`)
        const LHRT5Historic: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fHistoricStart}/${fHistoricEnd}/T5?granularity=${interval}&port-codes=LHR`)

        current = [...current, ...LHRT2.data, ...LHRT3.data, ...LHRT4.data, ...LHRT5.data]
        historic = [...historic, ...LHRT2Historic.data, ...LHRT3Historic.data, ...LHRT4Historic.data, ...LHRT5Historic.data]
      }
    }

    if (action.isExport) {
      const currentCSV = generateCsv({})(createExportableDatapoints(current))
      const historicCSV = generateCsv({})(createExportableDatapoints(historic))
      download({})(currentCSV)
      download({})(historicCSV)
      yield(put(setStatus('done')))
    } else {

      const forecastData: PortsObject = {}
      const forecastTotals: PortTotals = {}
      const historicData: PortsObject = {}
      const historicTotals: PortTotals = {}

      current!.forEach((datapoint) => {
        const portIndex = datapoint.terminalName ? `${datapoint.portCode}-${datapoint.terminalName}` : datapoint.portCode
        datapoint.queueCounts!.forEach(passengerCount => {
          forecastTotals[portIndex] = (forecastTotals[portIndex] ? forecastTotals[portIndex] : 0) + passengerCount.count
        })
        forecastData[portIndex] ?
          forecastData[portIndex].push(datapoint)
          : forecastData[portIndex] = [datapoint]
      })

      historic!.forEach((datapoint) => {
        const portIndex = datapoint.terminalName ? `${datapoint.portCode}-${datapoint.terminalName}` : datapoint.portCode
        datapoint.queueCounts!.forEach(passengerCount => {
          historicTotals[portIndex] = (historicTotals[portIndex] ? historicTotals[portIndex] : 0) + passengerCount.count
        })
        historicData[portIndex] ?
          historicData[portIndex].push(datapoint)
          : historicData[portIndex] = [datapoint]
      })

      yield(put(setRegionalDashboardState({
        singleOrRange: action.singleOrRange,
        comparisonType: action.comparisonType,
        interval: duration >= 48 ? 'day' : 'hour',
        forecastData: forecastData,
        forecastTotals: forecastTotals,
        historicData: historicData,
        historicTotals: historicTotals,
        forecastStart: forecastStart,
        forecastEnd: forecastEnd,
        status: 'done',
        historicStart: fHistoricStart,
        historicEnd: fHistoricEnd,
      })))

    }

  } catch (e) {
    console.log(e)
  }
}

export function* requestPaxTotalsSaga() {
  yield takeEvery('REQUEST_PAX_TOTALS', handleRequestPaxTotals)
}
