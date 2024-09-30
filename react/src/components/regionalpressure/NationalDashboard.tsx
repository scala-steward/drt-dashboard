import * as React from 'react';
import {connect, MapDispatchToProps} from 'react-redux';
import {
  Box,
  Grid,
  CircularProgress,
  Typography
} from "@mui/material";
import {UserProfile} from "../../model/User";
import {ConfigValues, PortRegion} from "../../model/Config";
import {RootState} from '../../store/redux';
import {requestPaxTotals} from './regionalPressureSagas';
import {FormError} from '../../services/ValidationService';
import RegionalPressureDates from './RegionalPressureDates';
import RegionalPressureChart from './RegionalPressureChart';
import RegionalPressureExport from './RegionalPressureExport';
import RegionalPressureForm from './RegionalPressureForm';
import {Helmet} from "react-helmet";
import {customerPageTitleSuffix} from "../../utils/common";

interface NationalDashboardProps {
  user: UserProfile;
  config: ConfigValues;
  errors: FormError[];
  status: string;
  type?: string;
  start?: string;
  end?: string;
}

const NationalDashboard = ({config, user, status}: NationalDashboardProps) => {

  let userPortsByRegion: PortRegion[] = config.portsByRegion.map(region => {
    const userPorts: string[] = user.ports.filter(p => region.ports.includes(p));
    return {...region, ports: userPorts} as PortRegion
  }).filter(r => r.ports.length > 0)

  const availablePorts = config.ports.map(port => port.iata);

  return <>
    <Helmet>
      <title>National Dashboard {customerPageTitleSuffix}</title>
    </Helmet>
    <Box sx={{backgroundColor: '#E6E9F1', p: 2}}>
      <Box sx={{mb: 4}}>
        <Typography variant='h1' sx={{mb: 3}}>National Dashboard</Typography>
        <Typography variant='h3' component='h2'>Compare pax arrivals with previous year</Typography>
      </Box>

      <RegionalPressureForm ports={user.ports} availablePorts={availablePorts} type="single"/>

      {status === 'loading' && <Grid container justifyContent={"center"}>
        <Grid item sx={{p: 4}}>
          <CircularProgress/>
        </Grid>
      </Grid>
      }

      {status !== 'loading' && <Grid container columnSpacing={2} justifyItems='stretch'>
        <Grid item xs={12}>
          <h2>Regional Overview</h2>
        </Grid>
        <Grid item xs={12} sm={9}>
          <RegionalPressureDates/>
        </Grid>
        <Grid item xs={12} sm={3} style={{textAlign: 'right'}}>
          <RegionalPressureExport/>
        </Grid>
        {userPortsByRegion.map(region => {
          const regionPorts = region.name === 'Heathrow' ? ['LHR-T2', 'LHR-T2', 'LHR-T4', 'LHR-T5'] : region.ports
          return <Grid key={region.name} item xs={12} md={6} sx={{mt: 2}}>
            <RegionalPressureChart regionName={region.name} portCodes={regionPorts}/>
          </Grid>
        })}
      </Grid>}
    </Box>
  </>
}
const mapDispatch = (dispatch: MapDispatchToProps<any, NationalDashboardProps>) => {
  return {
    requestRegionExport: (userPorts: string[], availablePorts: string[], searchType: string, startDate: string, endDate: string, isExport: boolean) => {
      dispatch(requestPaxTotals(userPorts, availablePorts, searchType, startDate, endDate, isExport));
    }
  };
};

const mapState = (state: RootState) => {
  return {
    errors: state.pressureDashboard?.errors,
    type: state.pressureDashboard?.type,
    startDate: state.pressureDashboard?.start,
    endDate: state.pressureDashboard?.end,
    status: state.pressureDashboard?.status,
  };
}


export default connect(mapState, mapDispatch)(NationalDashboard);
