import * as React from 'react'
import {connect, MapDispatchToProps} from 'react-redux'
import {RootState} from '../../store/redux'
import {FormControl, FormControlLabel, FormLabel, Grid, Radio, RadioGroup} from '@mui/material'
import {DatePicker} from '@mui/x-date-pickers/DatePicker'
import {getHistoricDateByDay, requestPaxTotals} from './regionalPressureSagas'
import moment, {Moment} from 'moment'
import {ErrorFieldMapping, FormError} from '../../services/ValidationService'

interface RegionalPressureFormProps {
  errors: FormError[]
  ports: string[]
  availablePorts: string[]
  singleOrRange: 'single' | 'range'
  initialComparisonType: 'previousYear' | 'custom',
  forecastStart: string
  forecastEnd: string
  historicStart: string
  historicEnd: string
  status: string
  requestRegion: (ports: string[], availablePorts: string[], singleOrRange: 'single' | 'range', comparisonType: 'previousYear' | 'custom', forecastStart: string, forecastEnd: string, isExport: boolean, historicStart: string, historicEnd: string) => void
}

interface RegionalPressureDatesState {
  start: Moment
  end: Moment
}

const RegionalPressureForm = ({
                                ports,
                                errors,
                                availablePorts,
                                forecastStart,
                                forecastEnd,
                                historicStart,
                                historicEnd,
                                singleOrRange,
                                initialComparisonType,
                                requestRegion
                              }: RegionalPressureFormProps) => {
  const [searchType, setSearchType] = React.useState<'single' | 'range'>(singleOrRange)
  const [comparisonType, setComparisonType] = React.useState<'previousYear' | 'custom'>(initialComparisonType)
  const [forecastDates, setForecastDates] = React.useState<RegionalPressureDatesState>({
    start: moment(forecastStart),
    end: moment(forecastEnd),
  })
  const [historicDates, setHistoricDates] = React.useState<RegionalPressureDatesState>({
    start: moment(historicStart),
    end: moment(historicEnd),
  })
  const errorFieldMapping: ErrorFieldMapping = {}
  errors.forEach((error: FormError) => errorFieldMapping[error.field] = true)

  React.useEffect(() => {
    requestRegion(
      ports,
      availablePorts,
      searchType,
      comparisonType,
      forecastDates.start.format('YYYY-MM-DD'),
      forecastDates.end.format('YYYY-MM-DD'),
      false,
      historicDates.start.format('YYYY-MM-DD'),
      historicDates.end.format('YYYY-MM-DD'),
    )
  }, [])

  const handleSearchTypeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const singleOrRange = event.target.value as 'single' | 'range';
    setSearchType(singleOrRange)
    event.preventDefault()
    requestRegion(
      ports,
      availablePorts,
      singleOrRange,
      comparisonType,
      forecastDates.start.format('YYYY-MM-DD'),
      forecastDates.end.format('YYYY-MM-DD'),
      false,
      historicDates.start.format('YYYY-MM-DD'),
      historicDates.end.format('YYYY-MM-DD'),
    )
  }

  const handleDateChange = (type: string, date: Moment) => {
    const forecastStart = type == 'start' ? date : forecastDates.start
    const forecastEnd = type == 'end' ? date : forecastDates.end
    const historicStart = comparisonType == 'previousYear' ? getHistoricDateByDay(forecastStart) : historicDates.start
    const historicEnd = comparisonType == 'previousYear' ? getHistoricDateByDay(forecastEnd) : historicDates.end

    setForecastDates({
      start: forecastStart,
      end: forecastEnd
    })

    setHistoricDates({
      start: historicStart,
      end: historicEnd
    })

    requestRegion(
      ports,
      availablePorts,
      searchType,
      comparisonType,
      forecastStart.format('YYYY-MM-DD'),
      forecastEnd.format('YYYY-MM-DD'),
      false,
      historicStart.format('YYYY-MM-DD'),
      historicEnd.format('YYYY-MM-DD'),
    )
  }

  const handleComparisonTypeChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    event.preventDefault()

    const comparisonType = event.target.value as 'previousYear' | 'custom'

    const historicStart = comparisonType === 'custom' ?
      historicDates.start :
      getHistoricDateByDay(forecastDates.start)

    const historicEnd = comparisonType === 'custom' ?
      historicDates.end :
      getHistoricDateByDay(forecastDates.end)

    setComparisonType(event.target.value as 'previousYear' | 'custom')
    setHistoricDates({
      start: historicStart,
      end: historicEnd
    })

    requestRegion(
      ports,
      availablePorts,
      searchType,
      comparisonType,
      forecastDates.start.format('YYYY-MM-DD'),
      forecastDates.end.format('YYYY-MM-DD'),
      false,
      historicStart.format('YYYY-MM-DD'),
      historicEnd.format('YYYY-MM-DD'),
    )
  }

  const handleComparisonDateChange = (type: string, comparisonDate: Moment) => {
    const duration = moment.duration(forecastDates.end.diff(forecastDates.start)).asHours()
    const comparisonEnd = moment(comparisonDate).add(duration, 'hours')

    setHistoricDates({
      start: comparisonDate,
      end: comparisonEnd
    })

    requestRegion(
      ports,
      availablePorts,
      searchType,
      comparisonType,
      forecastDates.start.format('YYYY-MM-DD'),
      forecastDates.end.format('YYYY-MM-DD'),
      false,
      comparisonDate.format('YYYY-MM-DD'),
      comparisonEnd.format('YYYY-MM-DD'),
    )
  }

  return (
    <>
      <Grid container spacing={2} justifyItems={'stretch'} sx={{mb: 2}}>
        <Grid item xs={12}>
          <FormControl>
            <FormLabel id="date-label">Select date</FormLabel>
            <RadioGroup
              row
              aria-labelledby="date-label"
              onChange={handleSearchTypeChange}
            >
              <FormControlLabel value="single" control={<Radio checked={searchType === 'single'}/>}
                                label="Single date"/>
              <FormControlLabel value="range" control={<Radio checked={searchType === 'range'}/>} label="Date range"/>
            </RadioGroup>
          </FormControl>
        </Grid>
      </Grid>
      <Grid container spacing={2} justifyItems={'stretch'} sx={{mb: 2}}>
        <Grid item>
          <DatePicker
            slotProps={{
              textField: {error: errorFieldMapping.startDate}
            }}
            label={searchType == 'single' ? "Date" : "From"}
            sx={{backgroundColor: '#fff', marginRight: '10px'}}
            value={forecastDates.start}
            onChange={(newValue: Moment | null) => handleDateChange('start', newValue || moment())}/>
        </Grid>
        {searchType === 'range' &&
          <Grid item>
            <DatePicker
              slotProps={{
                textField: {error: errorFieldMapping.endDate}
              }}
              label="To"
              sx={{backgroundColor: '#fff'}}
              value={forecastDates.end}
              onChange={(newValue: Moment | null) => handleDateChange('end', newValue || moment())}/>
          </Grid>
        }
      </Grid>
      <Grid container spacing={2} justifyItems={'stretch'} sx={{mb: 2}}>
        <Grid item xs={12}>
          <FormControl>
            <FormLabel id="date-label">Select comparison date</FormLabel>
            <RadioGroup
              row
              aria-labelledby="date-label"
              onChange={handleComparisonTypeChange}
            >
              <FormControlLabel value="previousYear" control={<Radio checked={comparisonType === 'previousYear'}/>}
                                label="Previous Year"/>
              <FormControlLabel value="custom" control={<Radio checked={comparisonType === 'custom'}/>}
                                label={searchType == 'single' ? "Custom date" : "Custom date range"}/>
            </RadioGroup>
          </FormControl>
        </Grid>

      </Grid>
      {comparisonType === 'custom' &&
        <Grid container spacing={2} justifyItems={'stretch'} sx={{mb: 2}}>
          <Grid item>
            <DatePicker
              slotProps={{
                textField: {error: errorFieldMapping.startDate}
              }}
              label={searchType == 'single' ? "Date" : "From"}
              sx={{backgroundColor: '#fff', marginRight: '10px'}}
              value={historicDates.start}
              onChange={(newValue: Moment | null) => handleComparisonDateChange('start', newValue || moment())}/>
          </Grid>
          {searchType === 'range' && <Grid item>
            <DatePicker
              disabled={true}
              slotProps={{
                textField: {error: errorFieldMapping.endDate}
              }}
              label="To"
              sx={{backgroundColor: '#fff'}}
              value={historicDates.end}/>
          </Grid>}
        </Grid>
      }
    </>
  )
}

const mapDispatch = (dispatch: MapDispatchToProps<any, RegionalPressureFormProps>) => {
  return {
    requestRegion: (
      userPorts: string[],
      availablePorts: string[],
      singleOrRange: 'single' | 'range',
      initialComparisonType: 'previousYear' | 'custom',
      startDate: string,
      endDate: string,
      isExport: boolean,
      historicStart: string,
      historicEnd: string,
    ) => {
      dispatch(requestPaxTotals(userPorts, availablePorts, singleOrRange, initialComparisonType, startDate, endDate, isExport, historicStart, historicEnd))
    }
  }
}

const mapState = (state: RootState) => {
  return {
    errors: state.pressureDashboard?.errors,
    singleOrRange: state.pressureDashboard?.singleOrRange,
    initialComparisonType: state.pressureDashboard?.comparisonType,
    forecastStart: state.pressureDashboard?.forecastStart,
    forecastEnd: state.pressureDashboard?.forecastEnd,
    historicStart: state.pressureDashboard?.historicStart,
    historicEnd: state.pressureDashboard?.historicEnd,
    status: state.pressureDashboard?.status,
  }
}

export default connect(mapState, mapDispatch)(RegionalPressureForm)
