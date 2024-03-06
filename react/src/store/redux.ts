import { configureStore } from '@reduxjs/toolkit';
import downloadManagerSlice from '../components/downloadmanager/downloadManagerState';
import regionalPressureSlice from '../components/regionalpressure/regionalPressureState';
import { rootSaga } from './root-saga';
import createSagaMiddleware from 'redux-saga';


export const sagaMiddleware = createSagaMiddleware();

const store = configureStore({
  reducer: {
    downloadManager: downloadManagerSlice,
    pressureDashboard: regionalPressureSlice,
  },
  middleware: (getDefaultMiddlware) =>  getDefaultMiddlware().prepend(sagaMiddleware),
});


sagaMiddleware.run(rootSaga)

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch


export default store;
