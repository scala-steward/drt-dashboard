import * as React from 'react';
import {connect} from 'react-redux'
import {RootState} from '../../store/redux';
import moment from 'moment';
import { getHistoricDateByDay } from './regionalPressureSagas';

interface RegionalPressureDateProps {
  start: string;
  end: string;
}

const RegionalPressureDates = ({start, end}: RegionalPressureDateProps) => {

  const historicStart = getHistoricDateByDay(moment(start));
  const historicEnd = getHistoricDateByDay(moment(end));
  return (
    <>
      <p style={{lineHeight: 1.2, margin: '0 0 1em 0'}}>
        <strong>Pax from selected date: </strong>{ moment(start).format('ddd Do MMM YYYY') }
        { start != end &&
          <span> to { moment(end).format('ddd Do MMM YYYY') }</span> 
        }
      </p>
      <p>
        <strong>Pax from previous year: </strong> { historicStart.format('ddd Do MMM YYYY') }
        { start != end &&
          <span> to { historicEnd.format('ddd Do MMM YYYY') }</span> 
        }
      </p>
    </>
  )
}

const mapState = (state: RootState) => {
  return { 
    start: state.pressureDashboard?.start,
    end: state.pressureDashboard?.end,
   };
}

export default connect(mapState)(RegionalPressureDates);
