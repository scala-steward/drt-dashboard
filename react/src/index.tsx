import React from 'react';
import './index.css';
import {App} from './App';
import {BrowserRouter} from "react-router-dom";
import {AdapterMoment} from '@mui/x-date-pickers/AdapterMoment';
import * as serviceWorker from './serviceWorker';
import {StyledEngineProvider} from '@mui/material/styles';
import {createRoot} from 'react-dom/client';
import 'moment/locale/en-gb';
import {LocalizationProvider} from "@mui/x-date-pickers/LocalizationProvider";
import store from './store/redux';
import { Provider } from 'react-redux';
import drtTheme from "./drtTheme";
import {ThemeProvider} from "@mui/material";

const container = document.getElementById('root');
const root = createRoot(container!);

console.log(store);

root.render(
  <React.StrictMode>
    <Provider store={store}>
      <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap"/>
      <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons"/>
      <StyledEngineProvider injectFirst>
        <BrowserRouter>
          <LocalizationProvider dateAdapter={AdapterMoment} adapterLocale={'en-gb'}>
              <StyledEngineProvider injectFirst>
                  <ThemeProvider  theme={drtTheme}>
                    <App/>
                  </ThemeProvider>
              </StyledEngineProvider>
          </LocalizationProvider>
        </BrowserRouter>
      </StyledEngineProvider>
    </Provider>
  </React.StrictMode>
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
