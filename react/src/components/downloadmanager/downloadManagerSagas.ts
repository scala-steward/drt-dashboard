import { put, call, takeEvery } from 'redux-saga/effects'
import { setStatus, setCreatedAt } from './downloadManagerState';
import ApiClient from '../../services/ApiClient';
import axios from 'axios';

export type RequestDownloadActionType = {
  type: "REQUEST_DOWNLOAD",
  ports: PortTerminal[], 
  breakdown: string, 
  startDate: string, 
  endDate: string,
};
export type PortTerminal = {
  port: string, 
  terminals: string[], 
};

type RequestDownloadPayload = {
  ports: PortTerminal[], 
  exportType: string, 
  startDate: string, 
  endDate: string,
};

export const requestDownload = (ports: PortTerminal[], breakdown: string, startDate: string, endDate: string) :RequestDownloadActionType => {
  return {
    "type": "REQUEST_DOWNLOAD",
    ports,
    breakdown,
    startDate,
    endDate
  };
};

function* handleRequestDownload(action: RequestDownloadActionType) {
  try {
    yield put(setStatus('requested'))

    const payload : RequestDownloadPayload = {
      ports: action.ports,
      exportType: 'arrivals',
      startDate: action.startDate,
      endDate: action.endDate
    }
    
    // @ts-expect-error - yield generator always has 'any' type
    const response = yield call (axios.post, ApiClient.exportEndpoint, payload)
    
    yield put(setCreatedAt(response.data.createdAt))
    yield put(setStatus(response.data.status))
  } catch (e) {
    console.log(e)
  }
}

export function* requestDownloadSaga() {
  yield takeEvery('REQUEST_DOWNLOAD', handleRequestDownload)
}

export type CheckDownloadStatusType = {
  type: "CHECK_DOWNLOAD_STATUS",
  createdAt: string,
};

export const checkDownloadStatus = (createdAt: string) :CheckDownloadStatusType => {
  return {
    "type": "CHECK_DOWNLOAD_STATUS",
    createdAt,
  };
};

function* handleCheckDownloadStatus(action: CheckDownloadStatusType) {
  try {

    // @ts-expect-error - yield generator always has 'any' type
    const response = yield call (axios.get, `${ApiClient.exportEndpoint}/${action.createdAt}`)
    if (response.data.status === 'complete') {
      yield put(setStatus(response.data.status))
    }
  } catch (e) {
    console.log(e)
  }
}

export function* checkDownloadStatusSaga() {
  yield takeEvery('CHECK_DOWNLOAD_STATUS', handleCheckDownloadStatus)
}
