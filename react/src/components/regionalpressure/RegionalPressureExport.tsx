import * as React from 'react';
import {connect} from 'react-redux'
import {RootState} from '../../store/redux';
import { Button } from '@mui/material';
import { TerminalDataPoint } from './regionalPressureSagas';
import { mkConfig, generateCsv, download } from "export-to-csv";
import { PortsObject } from './regionalPressureSagas';
import ArrowDownward from '@mui/icons-material/ArrowDownward';
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
  totalPcpPax: number, 
  terminalName?: string,
  EEA: number,
  eGates: number,
  nonEEA: number,
}

const results_to_array = (data: PortsObject, is_hourly: boolean) => {
  let data_rows: ExportDataPoint[] = []
  Object.keys(data).map((port: string) => {
    data[port].map((portDataPoint: TerminalDataPoint) => {

      let date = is_hourly ? 
        moment(portDataPoint.date).add(portDataPoint.hour, 'hours').format('YYYY-MM-DD HH:mm') :
        portDataPoint.date

      let exportDataPoint: ExportDataPoint = {
        date,
        portCode: portDataPoint.portCode || '',
        regionName: portDataPoint.regionName || '',
        totalPcpPax: portDataPoint.totalPcpPax || 0,
        terminalName: portDataPoint.terminalName || '',
        EEA: portDataPoint.queueCounts[0]?.count || 0,
        eGates: portDataPoint.queueCounts[1]?.count || 0,
        nonEEA: portDataPoint.queueCounts[2]?.count || 0,
      }
      data_rows.push(exportDataPoint)
    })
  })
  return data_rows
}


const RegionalPressureExport = ({portData, historicPortData, granularity}: RegionalPressureExportProps) => {

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

  return <Button startIcon={<ArrowDownward />} variant="outlined" sx={{backgroundColor: '#fff'}} onClick={handleExport}>Export</Button>
}


const mapState = (state: RootState) => {
  return { 
    portData: state.pressureDashboard?.portData,
    historicPortData: state.pressureDashboard?.historicPortData,
    granularity: state.pressureDashboard?.interval,
   };
}

export default connect(mapState)(RegionalPressureExport);
