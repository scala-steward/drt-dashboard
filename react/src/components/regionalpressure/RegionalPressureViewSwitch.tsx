import * as React from 'react';
import {connect} from 'react-redux';
import {
  Box,
  Grid,
  Button,
  CircularProgress
} from "@mui/material";
import {RootState} from '../../store/redux';
import { FormError } from '../../services/ValidationService';
import RegionalDashboardDetail from './RegionalDashboardDetail';
import RegionalPressureDates from './RegionalPressureDates';
import RegionalPressureChart from './ RegionalPressureChart';
import { ConfigValues, PortRegion } from '../../model/Config';

interface RegionalPressureViewSwitchProps {
  config: ConfigValues;
  status: string;
  errors: FormError[];
  userPortsByRegion: PortRegion[];
}

const RegionalPressureViewSwitch = ({config, status, userPortsByRegion}: RegionalPressureViewSwitchProps) => {
  const [region, setRegion] = React.useState<string>('overview');

  return (
    <Box>
      { status === 'loading' && <CircularProgress /> }
      { status !== 'loading' && region !== 'overview' && <RegionalDashboardDetail config={config} region={region} setRegion={setRegion} /> }
      { status !== 'loading' && region === 'overview' && 
      <Box>
        <Grid container columnSpacing={2} justifyItems='stretch'>
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
            return <Grid key={region.name} item xs={12} md={6} lg={3} sx={{mt: 2}}>
              <RegionalPressureChart regionName={region.name} portCodes={region.ports} onMoreInfo={setRegion} />
            </Grid>
          })}
        </Grid>
      </Box>
      
      }
    </Box>
  )
  
}

const mapState = (state: RootState) => {
  return { 
    errors: state.pressureDashboard?.errors,
    status: state.pressureDashboard?.status,
   };
}


export default connect(mapState, )(RegionalPressureViewSwitch);
