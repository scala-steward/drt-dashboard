import * as React from 'react';
import {connect} from 'react-redux'
import {RootState} from '../../store/redux';
import moment from 'moment';

interface RegionalPressureDateProps {
  start: string;
  end: string;
  historicStart: string;
  historicEnd: string;
}

const RegionalPressureDates = ({start, end, historicStart, historicEnd}: RegionalPressureDateProps) => {

  return (
    <>
      <p style={{lineHeight: 1.2, margin: '0 0 1em 0'}}>
        <strong>Pax from selected date: </strong>{ moment(start).format('dddd D MMM YYYY') }
        { start != end &&
          <span> to { moment(end).format('dddd D MMM YYYY') }</span> 
        }
      </p>
      <p>
        <strong>Pax from previous year: </strong> { moment(historicStart).format('dddd D MMM YYYY') }
        { start != end &&
          <span> to { moment(historicEnd).format('dddd D MMM YYYY') }</span> 
        }
      </p>
    </>
  )
}

const mapState = (state: RootState) => {
  return { 
    start: state.pressureDashboard?.start,
    end: state.pressureDashboard?.end,
    historicStart: state.pressureDashboard?.historicStart,
    historicEnd: state.pressureDashboard?.historicEnd,
   };
}

export default connect(mapState)(RegionalPressureDates);
