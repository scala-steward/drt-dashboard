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
    endDate,
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
  terminalName?: string,
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
    const start = moment(action.startDate);
    const end = action.searchType === 'single' ? start : moment(action.endDate).endOf('day');
    const historicStart = moment(start).subtract(1, 'year').format('YYYY-MM-DD')
    const historicEnd = moment(end).subtract(1, 'year').format('YYYY-MM-DD')
    const duration = moment.duration(end.diff(start)).asHours();
    const interval = duration >= 48 ? 'daily' : 'hourly';
    const allPorts = ["NQY","INV","STN","BHD","MME","BFS","PIK","ABZ","LBA","MAN","GLA","LCY","BRS","LGW","HUY","EMA","EDI","CWL","NWI","EXT","SOU","SEN","LTN","LPL","LHR","BOH","NCL","BHX"];

    const fStart = start.format('YYYY-MM-DD');
    const fEnd = end.format('YYYY-MM-DD');

    let current: TerminalDataPoint[];
    let historic: TerminalDataPoint[];
    let currentResponse: Response;
    let historicResponse: Response;
    if (window.location.hostname.includes('localhost')) {
      //stub all data for local development
      
      current =  StubService.generatePortPaxSeries(fStart, fEnd, interval, 'region', allPorts)
      historic = StubService.generatePortPaxSeries(historicStart, historicEnd, interval, 'region', allPorts)
    } else if (window.location.hostname.includes('preprod')) {

      //on preprod stub data for ports that are not available.

      currentResponse = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}?granularity=${interval}&port-codes=${action.availablePorts.join()}`);
      historicResponse = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${historicStart}/${historicEnd}?granularity=${interval}&port-codes=${action.availablePorts.join()}`);

      const missingPorts = allPorts.filter(port => !action.userPorts.includes(port));
      const missingCurrent: TerminalDataPoint[] =  StubService.generatePortPaxSeries(fStart, fEnd, interval, 'region', missingPorts)
      const missingHistoric: TerminalDataPoint[] = StubService.generatePortPaxSeries(historicStart, historicEnd, interval, 'region', missingPorts)

      current = [...currentResponse.data, ...missingCurrent];
      historic = [...historicResponse.data, ...missingHistoric];
    }
    else {
      currentResponse = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}?granularity=${interval}&port-codes=${action.availablePorts.join()}`);
      historicResponse = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${historicStart}/${historicEnd}?granularity=${interval}&port-codes=${action.availablePorts.join()}`);

      current = currentResponse.data;
      historic = historicResponse.data;

      if (action.availablePorts.includes('LHR')) {
        const LHRT2: Response = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}/T2?granularity=${interval}&port-codes=LHR`);
        const LHRT3: Response = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}/T3?granularity=${interval}&port-codes=LHR`);
        const LHRT4: Response = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}/T4?granularity=${interval}&port-codes=LHR`);
        const LHRT5: Response = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${fStart}/${fEnd}/T5?granularity=${interval}&port-codes=LHR`);

        const LHRT2Historic: Response = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${historicStart}/${historicEnd}/T2?granularity=${interval}&port-codes=LHR`);
        const LHRT3Historic: Response = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${historicStart}/${historicEnd}/T3?granularity=${interval}&port-codes=LHR`);
        const LHRT4Historic: Response = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${historicStart}/${historicEnd}/T4?granularity=${interval}&port-codes=LHR`);
        const LHRT5Historic: Response = yield call (axios.get, `${ApiClient.passengerTotalsEndpoint}${historicStart}/${historicEnd}/T5?granularity=${interval}&port-codes=LHR`);

        current = [...current, ...LHRT2.data, ...LHRT3.data, ...LHRT4.data, ...LHRT5.data ]
        historic = [...historic, ...LHRT2Historic.data, ...LHRT3Historic.data, ...LHRT4Historic.data, ...LHRT5Historic.data ]
      }
    }

    const portData: PortsObject = {};
    const portTotals: PortTotals = {};
    const historicPortData: PortsObject = {};
    const historicPortTotals: PortTotals = {};

    current!.forEach((datapoint) => {
      const portIndex = datapoint.terminalName ? `${datapoint.portCode}-${datapoint.terminalName}` : datapoint.portCode;
      datapoint.queueCounts.forEach(passengerCount => {
        portTotals[portIndex] = (portTotals[portIndex] ? portTotals[portIndex] : 0) + passengerCount.count
      })
      portData[portIndex] ?
      portData[portIndex].push(datapoint)
        : portData[portIndex] = [datapoint]
    })

    historic!.forEach((datapoint) => {
      const portIndex = datapoint.terminalName ? `${datapoint.portCode}-${datapoint.terminalName}` : datapoint.portCode;
      datapoint.queueCounts.forEach(passengerCount => {
        historicPortTotals[portIndex] = (historicPortTotals[portIndex] ? historicPortTotals[portIndex] : 0) + passengerCount.count
      })
      historicPortData[portIndex] ?
      historicPortData[portIndex].push(datapoint)
        : historicPortData[portIndex] = [datapoint]
    })
    
    yield(put(setRegionalDashboardState({
      portData,
      portTotals,
      historicPortData,
      historicPortTotals,
      type: action.searchType,
      start: fStart,
      end: fEnd,
      interval: duration >= 48 ? 'day' : 'hour',
      status: 'done'
    })))

  } catch (e) {
    console.log(e)
  }
}

export function* requestPaxTotalsSaga() {
  yield takeEvery('REQUEST_PAX_TOTALS', handleRequestPaxTotals)
}
