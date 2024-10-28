import * as React from 'react';
import {connect, MapDispatchToProps} from 'react-redux'
import {RootState} from '../../store/redux';
import { Grid, FormControl, FormLabel, RadioGroup, FormControlLabel, Radio, TextField } from '@mui/material';
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import { requestPaxTotals } from './regionalPressureSagas';
import moment, {Moment} from 'moment';
import { FormError } from '../../services/ValidationService';
import { ErrorFieldMapping } from '../../services/ValidationService';
import { getHistoricDateByDay } from './regionalPressureSagas';

interface RegionalPressureFormProps {
  errors: FormError[];
  ports: string[];
  availablePorts: string[],
  type?: string,
  comparison?: string,
  start: string;
  end: string;
  status: string;
  requestRegion: (ports: string[], availablePorts: string[], searchType: string, startDate: string, endDate: string, isExport: boolean, historicStart: string, historicEnd: string) => void;
}

interface RegionalPressureDatesState {
  start: Moment;
  end: Moment;
}

const RegionalPressureForm = ({ports, errors, availablePorts, start, type, comparison, end, requestRegion}: RegionalPressureFormProps) => {
  const [searchType, setSearchType] = React.useState<string>(type || 'single');
  const [comparisonType, setComparisonType] = React.useState<string>(comparison || 'previousYear');
  const [dates, setDate] = React.useState<RegionalPressureDatesState>({
    start: moment(start),
    end: moment(end),
  });
  const [historicDate, setHistoricDate] = React.useState<RegionalPressureDatesState>({
    start: getHistoricDateByDay(dates.start),
    end: getHistoricDateByDay(dates.end),
  });
  const errorFieldMapping: ErrorFieldMapping = {}
  errors.forEach((error: FormError) => errorFieldMapping[error.field] = true);

  React.useEffect(() => {
    requestRegion(
      ports, 
      availablePorts, 
      searchType, 
      dates.start.format('YYYY-MM-DD'), 
      dates.end.format('YYYY-MM-DD'), 
      false,
      historicDate.start.format('YYYY-MM-DD'),
      historicDate.end.format('YYYY-MM-DD'),
    );
  }, [])

  const handleSearchTypeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchType(event.target.value);
    event.preventDefault();
    requestRegion(
      ports, 
      availablePorts, 
      event.target.value, 
      dates.start.format('YYYY-MM-DD'), 
      dates.end.format('YYYY-MM-DD'), 
      false,
      historicDate.start.format('YYYY-MM-DD'),
      historicDate.end.format('YYYY-MM-DD'),
    )
  };

  const handleDateChange = (type: string, date: Moment) => {
    setDate({
      ...dates,
      [type]: date
    });

    let duration;

    if (type == 'start') {
      duration = moment.duration(dates.end.diff(date)).asHours();
    } else {
      duration = moment.duration(date.diff(dates.start)).asHours();
    }

    let hStart;
    if (comparisonType == 'previousYear') {
      hStart = getHistoricDateByDay(date);
    } else {
      hStart = historicDate.start;
    }
    const comparisonEnd = moment(historicDate.start).add(duration, 'hours');
    setHistoricDate({
      start: hStart,
      end: comparisonEnd
    });

    if (type === 'start') {
      requestRegion(
        ports, 
        availablePorts,
        searchType, 
        date!.format('YYYY-MM-DD'), 
        dates.end.format('YYYY-MM-DD'), 
        false,
        hStart.format('YYYY-MM-DD'),
        historicDate.end.format('YYYY-MM-DD'),
      );
    } else {
      requestRegion(
        ports,
        availablePorts, 
        searchType, 
        dates.start.format('YYYY-MM-DD'), 
        date!.format('YYYY-MM-DD'), 
        false,
        hStart.format('YYYY-MM-DD'),
        comparisonEnd.format('YYYY-MM-DD'),
      );
    }
  }

  const handleComparisonTypeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setComparisonType(event.target.value);
    event.preventDefault();

    let hStart = historicDate.start;
    let hEnd = historicDate.end;

    if (event.target.value == 'previousYear') {
      hStart = getHistoricDateByDay(dates.start);
      hEnd = getHistoricDateByDay(dates.end);
      setHistoricDate({
        start: hStart,
        end: hEnd
      });
    }

    requestRegion(
      ports, 
      availablePorts, 
      searchType, 
      dates.start.format('YYYY-MM-DD'), 
      dates.end.format('YYYY-MM-DD'), 
      false,
      hStart.format('YYYY-MM-DD'),
      hEnd.format('YYYY-MM-DD'),
    )
  };

  const handleComparisonDateChange = (type: string, comparisonDate: Moment) => {
    const duration = moment.duration(dates.end.diff(dates.start)).asHours();
    const comparisonEnd = moment(comparisonDate).add(duration, 'hours');
    
    setHistoricDate({
      start: comparisonDate,
      end: comparisonEnd
    });
    requestRegion(
      ports, 
      availablePorts, 
      searchType, 
      dates.start.format('YYYY-MM-DD'), 
      dates.end.format('YYYY-MM-DD'), 
      false,
      comparisonDate.format('YYYY-MM-DD'), 
      comparisonEnd.format('YYYY-MM-DD'), 
    )
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
          onChange={(newValue: Moment | null) => handleDateChange('start', newValue || moment())}/>
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
            onChange={(newValue: Moment | null) => handleDateChange('end', newValue || moment())}/>
          </Grid>
      }
    </Grid>
    <Grid container spacing={2} justifyItems={'stretch'} sx={{mb:2}}>
        <Grid item xs={12}>
          <FormControl>
            <FormLabel id="date-label">Select comparison date</FormLabel> 
            <RadioGroup
              row
              aria-labelledby="date-label"
              onChange={handleComparisonTypeChange}
            >
              <FormControlLabel value="previousYear" control={<Radio checked={comparisonType === 'previousYear'} />} label="Previous Year" />
              <FormControlLabel value="custom" control={<Radio checked={comparisonType === 'custom'} />} label={searchType == 'single' ? "Custom date" : "Custom date range"} />
            </RadioGroup>
          </FormControl>
        </Grid>

    </Grid>
        { comparisonType === 'custom' &&
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
                value={historicDate.start}
                onChange={(newValue: Moment | null) => handleComparisonDateChange('start', newValue || moment())}/>
            </Grid>
            { searchType === 'range' && <Grid item>
              <DatePicker 
                slots={{
                  textField: TextField
                }}
                disabled={true}
                slotProps={{
                  textField: { error: !!errorFieldMapping.endDate }
                }}
                label="To"  
                sx={{backgroundColor: '#fff'}}
                value={historicDate.end}/>
            </Grid> }
          </Grid>
        }
  </>
  )
}

const mapDispatch = (dispatch :MapDispatchToProps<any, RegionalPressureFormProps>) => {
  return {
    requestRegion: (
      userPorts: string[], 
      availablePorts: string[], 
      searchType: string, 
      startDate: string, 
      endDate: string,  
      isExport: boolean, 
      historicStart: string,
      historicEnd: string,
    ) => {
      dispatch(requestPaxTotals(userPorts, availablePorts, searchType, startDate, endDate, isExport, historicStart, historicEnd));
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
