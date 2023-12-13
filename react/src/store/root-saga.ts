
import { all } from 'redux-saga/effects'

import {
  requestDownloadSaga,
  checkDownloadStatusSaga
} from '../components/downloadmanager/downloadManagerSagas';

export function* rootSaga() :any {
  yield all([
    requestDownloadSaga(),
    checkDownloadStatusSaga()
  ]);
}

