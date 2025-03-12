import * as React from 'react';
import {connect} from 'react-redux'
import {RootState} from '../../store/redux';
import {useNavigate} from 'react-router';
import {Button, ButtonGroup} from '@mui/material';
import {paxByGateType, TerminalDataPoint} from './regionalPressureSagas';
import {mkConfig, generateCsv, download} from "export-to-csv";
import {PortsObject} from './regionalPressureSagas';
import ArrowDownward from '@mui/icons-material/ArrowDownward';
import BrowserUpdatedIcon from '@mui/icons-material/BrowserUpdated';
import moment from 'moment';

interface RegionalPressureExportProps {
  granularity: 'hour' | 'day',
  forecastHourlyPaxByPort: {
    [key: string]: TerminalDataPoint[]
  };
  historicHourlyPaxByPort: {
    [key: string]: TerminalDataPoint[]
  };
}

type ExportDataPoint = {
  forecastDate: string,
  historicDate: string,
  portCode: string,
  regionName: string,
  terminalName?: string,
  drtTotalPax: number,
  drtEgatePax: number,
  drtDeskPax: number,
  bxTotalPax: number,
  bxEgatePax: number,
  bxDeskPax: number,
}

const constructCsvRows = (forecast: PortsObject, historic: PortsObject, granularity: 'hour' | 'day') => {
  const rows: ExportDataPoint[] = []
  Object.keys(forecast).map((port: string) => {
    forecast[port]
      .filter((portDataPoint) => !portDataPoint.terminalName)
      .map((portDataPoint, index) => {
        const historicDataPoint = historic[port][index]
        const [drtEgatePax, drtDeskPax] = paxByGateType(portDataPoint.drtQueueCounts)
        const [bxEgatePax, bxDeskPax] = paxByGateType(historicDataPoint.bxQueueCounts)


        const date = granularity === 'hour' ?
          moment(portDataPoint.date).add(portDataPoint.hour, 'hours').format('HH:mm DD-MM-YYYY') :
          moment(portDataPoint.date).format('DD-MM-YYYY')

        const historicDate = granularity === 'hour' ?
          moment(historicDataPoint.date).add(historicDataPoint.hour, 'hours').format('HH:mm DD-MM-YYYY') :
          moment(historicDataPoint.date).format('DD-MM-YYYY')

        const exportDataPoint: ExportDataPoint = {
          forecastDate: date,
          historicDate,
          portCode: portDataPoint.portCode || '',
          regionName: portDataPoint.regionName || '',
          drtTotalPax: drtEgatePax + drtDeskPax,
          drtEgatePax: drtEgatePax,
          drtDeskPax: drtDeskPax,
          bxTotalPax: bxEgatePax + bxDeskPax,
          bxEgatePax: bxEgatePax,
          bxDeskPax: bxDeskPax,
        }
        rows.push(exportDataPoint)
      })
  })

  return rows
}

const RegionalPressureExport = ({forecastHourlyPaxByPort, historicHourlyPaxByPort, granularity}: RegionalPressureExportProps) => {

  const navigate = useNavigate();

  const csvConfig = mkConfig({
    filename: `regional-pressure-export`,
    useKeysAsHeaders: false,
    columnHeaders: [
      {key: 'forecastDate', displayLabel: 'Forecast date'},
      {key: 'historicDate', displayLabel: 'Historical date'},
      {key: 'portCode', displayLabel: 'Port code'},
      {key: 'regionName', displayLabel: 'Region name'},
      {key: 'drtTotalPax', displayLabel: 'DRT total pax'},
      {key: 'drtEgatePax', displayLabel: 'DRT e-gate pax'},
      {key: 'drtDeskPax', displayLabel: 'DRT desk pax'},
      {key: 'bxTotalPax', displayLabel: 'BX total pax'},
      {key: 'bxEgatePax', displayLabel: 'BX e-gate pax'},
      {key: 'bxDeskPax', displayLabel: 'BX desk pax'},
    ]
  });

  const handleExport = () => {
    const csvRows: ExportDataPoint[] = constructCsvRows(forecastHourlyPaxByPort, historicHourlyPaxByPort, granularity)
    const csv = generateCsv(csvConfig)(csvRows);
    download(csvConfig)(csv)
  }

  return <ButtonGroup sx={{width: '100%'}}>
    <Button
      fullWidth
      startIcon={<ArrowDownward />}
      variant="outlined"
      sx={{backgroundColor: '#fff'}}
      onClick={handleExport}>Export</Button>
    <Button
      fullWidth
      startIcon={<BrowserUpdatedIcon />}
      variant="outlined"
      sx={{backgroundColor: '#fff'}}
      onClick={() => navigate('/download')}>Download Manager</Button>
  </ButtonGroup>
}


const mapState = (state: RootState) => {
  return {
    forecastHourlyPaxByPort: state.pressureDashboard?.forecastHourlyPaxByPort,
    historicHourlyPaxByPort: state.pressureDashboard?.historicHourlyPaxByPort,
    granularity: state.pressureDashboard?.interval,
  };
}

export default connect(mapState)(RegionalPressureExport);
