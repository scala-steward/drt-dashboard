import * as React from 'react';
import {connect, MapDispatchToProps} from 'react-redux';
import {
  Box,
  Grid,
  FormControl,
  FormControlLabel,
  FormLabel,
  RadioGroup,
  Radio,
  TextField,
} from "@mui/material";
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import {UserProfile} from "../../model/User";
import {ConfigValues, PortRegion} from "../../model/Config";
import moment, {Moment} from 'moment';
import {RootState} from '../../store/redux';
import { FormError } from '../../services/ValidationService';

import { requestPaxTotals } from './regionalPressureSagas';
import RegionalPressureViewSwitch from './RegionalPressureViewSwitch';

interface RegionalPressureDashboardProps {
  user: UserProfile;
  config: ConfigValues;
  errors: FormError[];
  type?: string;
  start?: string;
  end?: string;
  requestRegion: (ports: string[], availablePorts: string[], searchType: string, startDate: string, endDate: string) => void;
}

interface ErrorFieldMapping {
  [key:string]: boolean
}

interface RegionalPressureDatesState {
  start: Moment;
  end: Moment;
}


const RegionalPressureDashboard = ({config, user, errors, type, start, end, requestRegion}: RegionalPressureDashboardProps) => {
  const [searchType, setSearchType] = React.useState<string>(type || 'single');
  const [dates, setDate] = React.useState<RegionalPressureDatesState>({
    start: moment(start).subtract(1, 'day'),
    end: moment(end),
  });
  const errorFieldMapping: ErrorFieldMapping = {}
  errors.forEach((error: FormError) => errorFieldMapping[error.field] = true);

  let userPortsByRegion: PortRegion[] = config.portsByRegion.map(region => {
    const userPorts: string[] = user.ports.filter(p => region.ports.includes(p));
    return {...region, ports: userPorts} as PortRegion
  }).filter(r => r.ports.length > 0)

  const availablePorts = config.ports.map(port => port.iata);

  React.useEffect(() => {
    requestRegion(user.ports, availablePorts, searchType, dates.start.format('YYYY-MM-DD'), dates.end.format('YYYY-MM-DD'));
  }, [user, availablePorts, requestRegion, searchType, dates])

  const handleSearchTypeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchType(event.target.value);
    event.preventDefault();
  };

  const handleDateChange = (type: string, date: Moment | null) => {
    setDate({
      ...dates,
      [type]: date
    });
    requestRegion(user.ports, availablePorts, searchType, date!.format('YYYY-MM-DD'), dates.end.format('YYYY-MM-DD'));
  }

  return (
    <Box>
      <Box sx={{backgroundColor: '#E6E9F1', p: 2}}>
        <Grid container spacing={2} justifyItems={'stretch'} sx={{mb:2}}>
            <Grid item xs={12}>
              <h1>National Dashboard</h1>
              <h2>Compare pax arrivals vs previous year</h2>
            </Grid>
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
                label="From" 
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
        
      
        <RegionalPressureViewSwitch config={config} userPortsByRegion={userPortsByRegion} />
      </Box>
    </Box>
  )
  
}

const mapState = (state: RootState) => {
  return { 
    errors: state.pressureDashboard?.errors,
    type: state.pressureDashboard?.type,
    startDate: state.pressureDashboard?.start,
    endDate: state.pressureDashboard?.end,
   };
}

const mapDispatch = (dispatch :MapDispatchToProps<any, RegionalPressureDashboardProps>) => {
  return {
    requestRegion: (userPorts: string[], availablePorts: string[], searchType: string, startDate: string, endDate: string) => {
      dispatch(requestPaxTotals(userPorts, availablePorts, searchType, startDate, endDate));
    }
  };
};

export default connect(mapState, mapDispatch)(RegionalPressureDashboard);
