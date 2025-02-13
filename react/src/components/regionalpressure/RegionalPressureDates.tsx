import * as React from 'react';
import {connect} from 'react-redux'
import {RootState} from '../../store/redux';
import moment from 'moment';

interface RegionalPressureDateProps {
  forecastStart: string;
  forecastEnd: string;
  historicStart: string;
  historicEnd: string;
}

const RegionalPressureDates = ({forecastStart, forecastEnd, historicStart, historicEnd}: RegionalPressureDateProps) => {

  return (
    <>
      <p style={{lineHeight: 1.2, margin: '0 0 1em 0'}}>
        <strong>Pax from selected date: </strong>{ moment(forecastStart).format('dddd D MMM YYYY') }
        { forecastStart != forecastEnd &&
          <span> to { moment(forecastEnd).format('dddd D MMM YYYY') }</span>
        }
      </p>
      <p>
        <strong>Pax from previous year: </strong> { moment(historicStart).format('dddd D MMM YYYY') }
        { forecastStart != forecastEnd &&
          <span> to { moment(historicEnd).format('dddd D MMM YYYY') }</span>
        }
      </p>
    </>
  )
}

const mapState = (state: RootState) => {
  return {
    forecastStart: state.pressureDashboard?.forecastStart,
    forecastEnd: state.pressureDashboard?.forecastEnd,
    historicStart: state.pressureDashboard?.historicStart,
    historicEnd: state.pressureDashboard?.historicEnd,
   };
}

export default connect(mapState)(RegionalPressureDates);
