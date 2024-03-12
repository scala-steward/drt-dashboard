import * as React from 'react';
import {connect} from 'react-redux';
import {
  Box,
  Grid,
  CircularProgress,
  Button
} from "@mui/material";
import {UserProfile} from "../../model/User";
import {ConfigValues, PortRegion} from "../../model/Config";
import {RootState} from '../../store/redux';
import { FormError } from '../../services/ValidationService';
import RegionalPressureDates from './RegionalPressureDates';
import RegionalPressureChart from './ RegionalPressureChart';

import RegionalPressureForm from './RegionalPressureForm';

interface RegionalPressureDashboardProps {
  user: UserProfile;
  config: ConfigValues;
  errors: FormError[];
  status: string;
  type?: string;
  start?: string;
  end?: string;
}

const RegionalPressureDashboard = ({config, user, status}: RegionalPressureDashboardProps) => {
  
  let userPortsByRegion: PortRegion[] = config.portsByRegion.map(region => {
    const userPorts: string[] = user.ports.filter(p => region.ports.includes(p));
    return {...region, ports: userPorts} as PortRegion
  }).filter(r => r.ports.length > 0)

  const availablePorts = config.ports.map(port => port.iata);

  return (
    <Box sx={{backgroundColor: '#E6E9F1', p: 2}}>
      <h1>National Dashboard</h1>
      <h2>Compare pax arrivals vs previous year</h2>
      
      <RegionalPressureForm ports={user.ports} availablePorts={availablePorts} type="single" />

      { status === 'loading' && <Grid container justifyContent={"center"}>
        <Grid item sx={{p: 4}}>
          <CircularProgress />
          </Grid>
        </Grid> 
      }
  
      { status !== 'loading' && <Grid container columnSpacing={2} justifyItems='stretch'>
        <Grid item xs={12}>
          <h2>Regional Overview</h2>
        </Grid>
        <Grid item xs={8}>
          <RegionalPressureDates />
        </Grid>
        <Grid item xs={4} style={{textAlign: 'right'}}>
          <Button variant="outlined" sx={{backgroundColor: '#fff'}}>Export</Button>
        </Grid>
        { userPortsByRegion.map(region => {
          const regionPorts = region.name === 'Heathrow' ? ['LHR-T2', 'LHR-T2', 'LHR-T4', 'LHR-T5'] : region.ports
          return <Grid key={region.name} item xs={12} md={6} lg={3} sx={{mt: 2}}>
            <RegionalPressureChart regionName={region.name} portCodes={regionPorts} />
          </Grid>
        })}
      </Grid>}
    </Box>
  )
  
}

const mapState = (state: RootState) => {
  return { 
    errors: state.pressureDashboard?.errors,
    type: state.pressureDashboard?.type,
    startDate: state.pressureDashboard?.start,
    endDate: state.pressureDashboard?.end,
    status: state.pressureDashboard?.status,
   };
}


export default connect(mapState)(RegionalPressureDashboard);
