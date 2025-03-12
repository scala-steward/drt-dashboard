import {call, put, takeEvery} from 'redux-saga/effects'
import {setRegionalDashboardState, setStatus} from './regionalPressureState'
import StubService from '../../services/stub-service'
import moment from 'moment/moment'
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
  bxQueueCounts: QueueCount[],
  date: string,
  hour: number,
  portCode: string,
  drtQueueCounts: QueueCount[],
  regionName: string,
  terminalName?: string,
}


export type ExportableDataPoint = {
  date: string,
  hour: number,
  portCode: string,
  regionName: string,
  terminalName?: string,
  drtEgatePax?: number,
  drtDeskPax?: number,
  bxEgatePax?: number,
  bXDeskPax?: number,
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


const createExportableDataPoints = (dataPoints: TerminalDataPoint[]): ExportableDataPoint[] => {
  let flattenedCurrent: ExportableDataPoint[] = []
  dataPoints!.forEach((datapoint) => {
    const [drtEgatePax, drtDeskPax] = paxByGateType(datapoint.drtQueueCounts)
    const [bxEgatePax, bXDeskPax] = paxByGateType(datapoint.bxQueueCounts)

    flattenedCurrent.push({
      date: datapoint.date,
      hour: datapoint.hour,
      portCode: datapoint.portCode,
      regionName: datapoint.regionName,
      terminalName: datapoint.terminalName,
      drtEgatePax: drtEgatePax,
      drtDeskPax: drtDeskPax,
      bxEgatePax: bxEgatePax,
      bXDeskPax: bXDeskPax,
    })
  })
  return flattenedCurrent
}

export function paxByGateType(counts: QueueCount[]): number[] {
  const egatePax = counts.filter((q) => q.queueName === 'EGate').map((q) => q.count).reduce((a, b) => a + b, 0)
  const deskPax = counts.filter((q) => q.queueName !== 'EGate').map((q) => q.count).reduce((a, b) => a + b, 0)
  return [egatePax, deskPax]
}

function* getPaxData(fStart: string, fEnd: string, interval: string, ports: string[]) {
  const currentResponse: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}?granularity=${interval}&port-codes=${ports.join()}`)
  const current = currentResponse.data as TerminalDataPoint[]

  if (ports.includes('LHR')) {
    const LHRT2: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}/T2?granularity=${interval}&port-codes=LHR`)
    const LHRT3: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}/T3?granularity=${interval}&port-codes=LHR`)
    const LHRT4: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}/T4?granularity=${interval}&port-codes=LHR`)
    const LHRT5: APIResponse = yield call(axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}/T5?granularity=${interval}&port-codes=LHR`)

    return [...current, ...LHRT2.data, ...LHRT3.data, ...LHRT4.data, ...LHRT5.data]
  } else {
    return current
  }
}

const parseDataPoints: (dataPoints: TerminalDataPoint[], paxNumber: (dp: TerminalDataPoint) => number) => [PortsObject, PortTotals] =
  (dataPoints, paxNumber) => {
    let totalPaxByPort: PortTotals = {}
    let hourlyPaxByPort: PortsObject = {}

    dataPoints.forEach((datapoint) => {
      const portIndex = datapoint.terminalName ? `${datapoint.portCode}-${datapoint.terminalName}` : datapoint.portCode

      totalPaxByPort[portIndex] = totalPaxByPort[portIndex] ? totalPaxByPort[portIndex] + paxNumber(datapoint) : paxNumber(datapoint)

      hourlyPaxByPort[portIndex] ?
        hourlyPaxByPort[portIndex].push(datapoint) :
        hourlyPaxByPort[portIndex] = [datapoint]
    })

    return [hourlyPaxByPort, totalPaxByPort]
  }


export function totalFromQueues(dp: QueueCount[]) {
  return dp.map((q) => q.count).reduce((a, b) => a + b, 0);
}

export function* handleRequestPaxTotals(action: RequestPaxTotalsType) {
  try {
    yield(put(setStatus('loading')))
    const start = moment(action.forecastStart)
    const end = action.singleOrRange === 'single' ? start : moment(action.forecastEnd).endOf('day')
    const historicStart = moment(action.historicStart)
    const historicEnd = action.singleOrRange === 'single' ? historicStart : moment(action.historicEnd).endOf('day')

    const forecastStart = start.format('YYYY-MM-DD')
    const forecastEnd = end.format('YYYY-MM-DD')
    const fHistoricStart = historicStart.format('YYYY-MM-DD')
    const fHistoricEnd = historicEnd.format('YYYY-MM-DD')
    const duration = moment.duration(end.diff(start)).asHours()
    const interval = duration >= 48 ? 'daily' : 'hourly'

    const useStub = process.env.REACT_APP_USE_STUB === 'true'

    const fetchPaxData = useStub ? StubService.generatePortPaxSeries : getPaxData

    const current: TerminalDataPoint[] = yield fetchPaxData(forecastStart, forecastEnd, interval, action.availablePorts)
    const historic: TerminalDataPoint[] = yield fetchPaxData(fHistoricStart, fHistoricEnd, interval, action.availablePorts)

    if (action.isExport) {
      [current, historic].map((data) => {
        download({})(generateCsv({})(createExportableDataPoints(data)))
      })
      yield(put(setStatus('done')))
    } else {

      const [forecastHourlyPaxByPort, forecastTotalPaxByPort] = parseDataPoints(current, (dp) => totalFromQueues(dp.drtQueueCounts))
      const [historicHourlyPaxByPort, historicTotalPaxByPort] = parseDataPoints(historic, (dp) => totalFromQueues(dp.bxQueueCounts))

      yield(put(setRegionalDashboardState({
        forecastHourlyPaxByPort: forecastHourlyPaxByPort,
        forecastTotalPaxByPort: forecastTotalPaxByPort,
        historicHourlyPaxByPort: historicHourlyPaxByPort,
        historicTotalPaxByPort: historicTotalPaxByPort,
        singleOrRange: action.singleOrRange,
        comparisonType: action.comparisonType,
        interval: duration >= 48 ? 'day' : 'hour',
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
