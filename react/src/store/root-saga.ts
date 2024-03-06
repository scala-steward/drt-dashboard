
import { all } from 'redux-saga/effects'

import {
  requestDownloadSaga,
  checkDownloadStatusSaga
} from '../components/downloadmanager/downloadManagerSagas';

import {
  requestPaxTotalsSaga,
} from '../components/regionalpressure/regionalPressureSagas';

export function* rootSaga() :any {
  yield all([
    requestDownloadSaga(),
    checkDownloadStatusSaga(),
    requestPaxTotalsSaga()
  ]);
}

