import * as React from 'react';
import {connect} from 'react-redux'
import {RootState} from '../../store/redux';
import { useNavigate } from 'react-router';
import { Button, ButtonGroup } from '@mui/material';
import {paxByGateType, TerminalDataPoint} from './regionalPressureSagas';
import { mkConfig, generateCsv, download } from "export-to-csv";
import { PortsObject } from './regionalPressureSagas';
import ArrowDownward from '@mui/icons-material/ArrowDownward';
import BrowserUpdatedIcon from '@mui/icons-material/BrowserUpdated';
import moment from 'moment';

interface RegionalPressureExportProps {
  granularity: string,
  portData: {
    [key: string]: TerminalDataPoint[]
  };
  historicPortData: {
    [key: string]: TerminalDataPoint[]
  };
}

type ExportDataPoint = {
  date: string,
  portCode: string,
  regionName: string,
  terminalName?: string,
  drtTotalPax: number,
  drtEgatePax: number,
  drtDeskPax: number,
  bxTotalPax: number,
  bxEgatePax: number,
  bXDeskPax: number,
}

const results_to_array = (data: PortsObject, is_hourly: boolean) => {
  const data_rows: ExportDataPoint[] = []
  Object.keys(data).map((port: string) => {
    data[port].map((portDataPoint: TerminalDataPoint) => {

      const [drtEgatePax, drtDeskPax] = paxByGateType(portDataPoint.drtQueueCounts)
      const [bxEgatePax, bXDeskPax] = paxByGateType(portDataPoint.bxQueueCounts)

      const date = is_hourly ?
        moment(portDataPoint.date).add(portDataPoint.hour, 'hours').format('YYYY-MM-DD HH:mm') :
        portDataPoint.date

      const exportDataPoint: ExportDataPoint = {
        date,
        portCode: portDataPoint.portCode || '',
        regionName: portDataPoint.regionName || '',
        terminalName: portDataPoint.terminalName || '',
        drtTotalPax: drtEgatePax + drtDeskPax,
        bxTotalPax: bxEgatePax + bXDeskPax,
        drtEgatePax: drtEgatePax,
        bxEgatePax: bxEgatePax,
        drtDeskPax: drtDeskPax,
        bXDeskPax: bXDeskPax,
      }
      data_rows.push(exportDataPoint)
    })
  })
  return data_rows
}

const RegionalPressureExport = ({portData, historicPortData, granularity}: RegionalPressureExportProps) => {

  const navigate = useNavigate();
  const is_hourly = granularity === 'hour'

  const csvConfig = mkConfig({
    useKeysAsHeaders: true
  });

  const handleExport = () => {
    const current_rows: ExportDataPoint[] = results_to_array(portData, is_hourly)
    const historic_rows: ExportDataPoint[] = results_to_array(historicPortData, is_hourly)
    const csv = generateCsv(csvConfig)([...historic_rows, ...current_rows]);
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
    portData: state.pressureDashboard?.currentHourlyPaxByPort,
    historicPortData: state.pressureDashboard?.historicHourlyPaxByPort,
    granularity: state.pressureDashboard?.interval,
   };
}

export default connect(mapState)(RegionalPressureExport);
