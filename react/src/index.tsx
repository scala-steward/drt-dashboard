import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import {BrowserRouter as Router} from "react-router-dom";
import {rootStore} from "./store/rootReducer";
import {Provider} from "react-redux";
import AdapterDateFns from '@mui/lab/AdapterDateFns';
import LocalizationProvider from '@mui/lab/LocalizationProvider';
import * as serviceWorker from './serviceWorker';
import {createMuiTheme, StyledEngineProvider, ThemeProvider} from '@mui/material/styles';

const theme = createMuiTheme();

ReactDOM.render(
  <React.StrictMode>
    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap"/>
    <link rel="stylesheet" href="https://fonts.googleapis.com/icon?family=Material+Icons"/>
    <StyledEngineProvider injectFirst>
      <Router>
        <LocalizationProvider dateAdapter={AdapterDateFns}>
          <Provider store={rootStore}>
            <ThemeProvider theme={theme}>
              <App/>
            </ThemeProvider>
          </Provider>
        </LocalizationProvider>
      </Router>
    </StyledEngineProvider>
  </React.StrictMode>,
  document.getElementById('root')
);

// If you want your app to work offline and load faster, you can change
// unregister() to register() below. Note this comes with some pitfalls.
// Learn more about service workers: https://bit.ly/CRA-PWA
serviceWorker.unregister();
