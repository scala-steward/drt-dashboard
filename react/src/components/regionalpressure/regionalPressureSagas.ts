import {  call, put, takeEvery } from 'redux-saga/effects';
import {setRegionalDashboardState, setStatus } from './regionalPressureState';
import StubService from '../../services/stub-service';
import moment from 'moment';
import ApiClient from '../../services/ApiClient';
import axios from 'axios';

export type RequestPaxTotalsType = {
  type: "REQUEST_PAX_TOTALS",
  searchType: string,
  userPorts: string[],
  availablePorts: string[],
  startDate: string,
  endDate: string,
};
export type PortTerminal = {
  port: string,
  ports: string[],
};

export const requestPaxTotals = (userPorts: string[], availablePorts: string[], searchType: string, startDate: string, endDate: string) :RequestPaxTotalsType => {
  return {
    "type": "REQUEST_PAX_TOTALS",
    searchType,
    userPorts,
    availablePorts,
    startDate,
    endDate
  };
};

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
};

export type PortsObject = {
  [key:string] :  TerminalDataPoint[]
}

export type PortTotals = {
  [key:string] : number
}

type Response = {
  data: TerminalDataPoint[]
}

function* handleRequestPaxTotals(action: RequestPaxTotalsType) {
  try {
    yield(put(setStatus('loading')))
    const start = moment(action.startDate).startOf('day').format('YYYY-MM-DD');
    const end = action.searchType === 'single' ? start : moment(action.endDate).startOf('day').format('YYYY-MM-DD');
    const historicStart = moment(start).subtract(1, 'year').format('YYYY-MM-DD')
    const historicEnd = moment(end).subtract(1, 'year').format('YYYY-MM-DD')
    const interval = action.searchType === 'single' ? 'hourly' : 'daily';
    const allPorts = ["NQY","INV","STN","BHD","MME","BFS","PIK","ABZ","LBA","MAN","GLA","LCY","BRS","LGW","HUY","EMA","EDI","CWL","NWI","EXT","SOU","SEN","LTN","LPL","LHR","BOH","NCL","BHX"];

    let current: TerminalDataPoint[];
    let historic: TerminalDataPoint[];
    let currentResponse: Response;
    let historicResponse: Response;
    if (window.location.hostname.includes('localhost')) {
      //stub all data for local development
      current =  StubService.generatePortPaxSeries(start, end, interval, 'region', allPorts)
      historic = StubService.generatePortPaxSeries(historicStart, historicEnd, interval, 'region', allPorts)
    } else if (window.location.hostname.includes('preprod')) {

      //on preprod stub data for ports that are not available.

      currentResponse = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${start}/${end}?granularity=${interval}&port-codes=${action.availablePorts.join()}`);
      historicResponse = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${historicStart}/${historicEnd}?granularity=${interval}&port-codes=${action.availablePorts.join()}`);

      const missingPorts = allPorts.filter(port => !action.userPorts.includes(port));
      const missingCurrent: TerminalDataPoint[] =  StubService.generatePortPaxSeries(start, end, interval, 'region', missingPorts)
      const missingHistoric: TerminalDataPoint[] = StubService.generatePortPaxSeries(historicStart, historicEnd, interval, 'region', missingPorts)

      current = [...currentResponse.data, ...missingCurrent];
      historic = [...historicResponse.data, ...missingHistoric];
    }
    else {
      currentResponse = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${start}/${end}?granularity=${interval}&port-codes=${action.availablePorts.join()}`);
      historicResponse = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${historicStart}/${historicEnd}?granularity=${interval}&port-codes=${action.availablePorts.join()}`);
      current = currentResponse.data;
      historic = historicResponse.data;
    }

    const portData: PortsObject = {};
    const portTotals: PortTotals = {};
    const historicPortData: PortsObject = {};
    const historicPortTotals: PortTotals = {};

    current!.forEach((datapoint) => {
      datapoint.queueCounts.forEach(passengerCount => {
        portTotals[datapoint.portCode] = (portTotals[datapoint.portCode] ? portTotals[datapoint.portCode] : 0) + passengerCount.count
      })
      portData[datapoint.portCode] ?
      portData[datapoint.portCode].push(datapoint)
        : portData[datapoint.portCode] = [datapoint]
    })

    historic!.forEach((datapoint) => {
      datapoint.queueCounts.forEach(passengerCount => {
        historicPortTotals[datapoint.portCode] = (historicPortTotals[datapoint.portCode] ? historicPortTotals[datapoint.portCode] : 0) + passengerCount.count
      })
      historicPortData[datapoint.portCode] ?
      historicPortData[datapoint.portCode].push(datapoint)
        : historicPortData[datapoint.portCode] = [datapoint]
    })
    yield(put(setRegionalDashboardState({
      portData,
      portTotals,
      historicPortData,
      historicPortTotals,
      type: action.searchType,
      start,
      end,
      interval: action.searchType === 'single' ? 'hour' : 'day',
      status: 'done'
    })))

  } catch (e) {
    console.log(e)
  }
}

export function* requestPaxTotalsSaga() {
  yield takeEvery('REQUEST_PAX_TOTALS', handleRequestPaxTotals)
}
