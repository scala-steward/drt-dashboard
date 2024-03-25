import * as React from 'react';
import {connect, MapDispatchToProps} from 'react-redux'
import {RootState} from '../../store/redux';
import { Grid, FormControl, FormLabel, RadioGroup, FormControlLabel, Radio, TextField } from '@mui/material';
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import { requestPaxTotals } from './regionalPressureSagas';
import moment, {Moment} from 'moment';
import { FormError } from '../../services/ValidationService';
import { ErrorFieldMapping } from '../../services/ValidationService';

interface RegionalPressureFormProps {
  errors: FormError[];
  ports: string[];
  availablePorts: string[],
  type: string,
  start: string;
  end: string;
  status: string;
  requestRegion: (ports: string[], availablePorts: string[], searchType: string, startDate: string, endDate: string, isExport: boolean) => void;
}

interface RegionalPressureDatesState {
  start: Moment;
  end: Moment;
}

const RegionalPressureForm = ({ports, errors, availablePorts, start, type, end, requestRegion}: RegionalPressureFormProps) => {
  const [searchType, setSearchType] = React.useState<string>(type || 'single');
  const [dates, setDate] = React.useState<RegionalPressureDatesState>({
    start: moment(start),
    end: moment(end),
  });
  const errorFieldMapping: ErrorFieldMapping = {}
  errors.forEach((error: FormError) => errorFieldMapping[error.field] = true);

  React.useEffect(() => {
    requestRegion(ports, availablePorts, searchType, dates.start.format('YYYY-MM-DD'), dates.end.format('YYYY-MM-DD'), false);
  }, [])

  const handleSearchTypeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchType(event.target.value);
    event.preventDefault();
    requestRegion(ports, availablePorts, event.target.value, dates.start.format('YYYY-MM-DD'), dates.end.format('YYYY-MM-DD'), false);
  };

  const handleDateChange = (type: string, date: Moment | null) => {
    setDate({
      ...dates,
      [type]: date
    });
    if (type === 'start') {
      requestRegion(ports, availablePorts, searchType, date!.format('YYYY-MM-DD'), dates.end.format('YYYY-MM-DD'), false);
    } else {
      requestRegion(ports, availablePorts, searchType, dates.start.format('YYYY-MM-DD'), date!.format('YYYY-MM-DD'), false);
    }
  }

  return (
    <>    
      <Grid container spacing={2} justifyItems={'stretch'} sx={{mb:2}}>
        <Grid item xs={12}>
          <FormControl>
            <FormLabel id="date-label">Select date</FormLabel> 
            <RadioGroup
              row
              aria-labelledby="date-label"
              onChange={handleSearchTypeChange}
            >
              <FormControlLabel value="single" control={<Radio checked={searchType === 'single'} />} label="Single date" />
              <FormControlLabel value="range" control={<Radio checked={searchType === 'range'} />} label="Date range" />
            </RadioGroup>
          </FormControl>
        </Grid>
    </Grid>
    <Grid container spacing={2} justifyItems={'stretch'} sx={{mb:2}}>
      <Grid item>
        <DatePicker 
          slots={{
            textField: TextField
          }}
          slotProps={{
            textField: { error: !!errorFieldMapping.startDate }
          }}
          label={searchType == 'single' ? "Date" : "From" }
          sx={{backgroundColor: '#fff', marginRight: '10px'}}
          value={dates.start}
          onChange={(newValue: Moment | null) => handleDateChange('start', newValue)}/>

        </Grid>
      { searchType === 'range' &&
        <Grid item>
          <DatePicker 
            slots={{
              textField: TextField
            }}
            slotProps={{
              textField: { error: !!errorFieldMapping.endDate }
            }}
            label="To"  
            sx={{backgroundColor: '#fff'}}
            value={dates.end}
            onChange={(newValue: Moment | null) => handleDateChange('end', newValue)}/>
          </Grid>
      }
    </Grid>
  </>
  )
}

const mapDispatch = (dispatch :MapDispatchToProps<any, RegionalPressureFormProps>) => {
  return {
    requestRegion: (userPorts: string[], availablePorts: string[], searchType: string, startDate: string, endDate: string,  isExport: boolean) => {
      dispatch(requestPaxTotals(userPorts, availablePorts, searchType, startDate, endDate, isExport));
    }
  };
};

const mapState = (state: RootState) => {
  return { 
    start: state.pressureDashboard?.start,
    end: state.pressureDashboard?.end,
    errors: state.pressureDashboard?.errors,
    status: state.pressureDashboard?.status,
   };
}

export default connect(mapState, mapDispatch)(RegionalPressureForm);
