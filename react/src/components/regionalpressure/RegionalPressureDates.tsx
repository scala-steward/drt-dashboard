import * as React from 'react';
import {connect} from 'react-redux'
import {RootState} from '../../store/redux';
import moment from 'moment';

interface RegionalPressureDateProps {
  start: string;
  end: string;
}

const RegionalPressureDates = ({start, end}: RegionalPressureDateProps) => {
  return (
    <>
      <p style={{lineHeight: 1.2, margin: '0 0 1em 0'}}>
        <strong>Pax from selected date: </strong>{ moment(start).format('ddd Do MMM YYYY') }
        { start != end &&
          <span>to { moment(end).format('ddd Do MMM YYYY') }</span> 
        }
      </p>
      <p>
        <strong>Pax from previous year: </strong> { moment(start).subtract(1,'y').format('ddd Do MMM YYYY') }
        { start != end &&
          <span>to { moment(end).subtract(1,'y').format('ddd Do MMM YYYY') }</span> 
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
