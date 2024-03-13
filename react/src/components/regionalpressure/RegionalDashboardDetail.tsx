import * as React from 'react';
import { connect } from 'react-redux';
import { UserProfile } from "../../model/User";
import { useParams } from 'react-router';
import {
  Alert,
  Box,
  Grid,
  Card,
  CardContent,
  CardHeader,
  Button,
  IconButton,
  Stack,
  FormControl,
  FormGroup,
  FormLabel,
  FormControlLabel,
  Checkbox
} from "@mui/material";
import { Link } from 'react-router-dom';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import { ConfigValues } from "../../model/Config";
import { RootState } from '../../store/redux';
import drtTheme from '../../drtTheme';
import { Line } from 'react-chartjs-2'; import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  TimeScale,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';
import 'chartjs-adapter-moment';
import moment from 'moment';
ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
);
import { ArrowBack } from '@mui/icons-material';
import { TerminalDataPoint } from './regionalPressureSagas';
import RegionalPressureDates from './RegionalPressureDates';
import RegionalPressureForm from './RegionalPressureForm';


interface RegionalPressureDetailProps {
  config: ConfigValues;
  user: UserProfile;
  title?: string;
  interval?: string;
  type: string;
  portData: {
    [key: string]: TerminalDataPoint[]
  };
  historicPortData: {
    [key: string]: TerminalDataPoint[]
  };
}

const RegionalPressureDetail = ({ config, portData, historicPortData, interval, type }: RegionalPressureDetailProps) => {
  const { region } = useParams() || '';
  let regionPorts = config.portsByRegion.filter((r) => r.name.toLowerCase() === region!.toLowerCase())[0].ports;
  if (region === 'heathrow') {
    regionPorts = ['LHR-T2', 'LHR-T3', 'LHR-T4', 'LHR-T5'];
  }
  const [visiblePorts, setVisiblePorts] = React.useState<string[]>([...regionPorts]);
  const availablePorts = config.ports.map(port => port.iata);
  const timeUnits = interval;

  const handleTogglePort = (port: string) => {
    if (visiblePorts.includes(port)) {
      const newPorts = [...visiblePorts];
      newPorts.splice(visiblePorts.indexOf(port), 1);
      setVisiblePorts(newPorts)
    } else {
      setVisiblePorts([
        ...visiblePorts,
        port
      ])
    }
  }

  return (
    <Box sx={{ backgroundColor: '#E6E9F1', p: 2 }}>
      <Grid container spacing={2} justifyItems={'stretch'} alignContent={'center'}>
        <Grid>
          <IconButton component={Link} to="/national-pressure" size='small' sx={{ margin: '16px 10px 0 16px' }}><ArrowBack /></IconButton>
        </Grid>
        <Grid>
          <h2 style={{ textTransform: 'capitalize' }}>{`${region} Region`}</h2>
        </Grid>
      </Grid>
      <Grid>
        <RegionalPressureForm ports={regionPorts} type={type} availablePorts={availablePorts} />
      </Grid>
      <Grid container spacing={2} justifyItems={'stretch'} sx={{ mb: 2 }}>
        <Grid item xs={10}>
          <RegionalPressureDates />
        </Grid>
        <Grid item xs={2}>
          <Stack direction="row-reverse" spacing={2}>
            <Button variant="outlined"><ArrowDownwardIcon />Export</Button>
          </Stack>
        </Grid>
        <Grid item xs={12}>
          <FormControl component="fieldset" variant="standard">
            <FormLabel component="legend">Airports:</FormLabel>
            <FormGroup>
              <Stack direction={'row'}>
                {
                  regionPorts && regionPorts.map((port: string) => {
                    return <FormControlLabel
                      key={port}
                      value={port}
                      control={<Checkbox checked={visiblePorts.includes(port)} value={port} />}
                      onClick={() => handleTogglePort(port)}
                      label={port.toUpperCase()}
                      labelPlacement="end"
                    />
                  })
                }
              </Stack>
            </FormGroup>
          </FormControl>
        </Grid>
        {regionPorts && regionPorts.map((port: string) => {
          return visiblePorts.includes(port) && portData[port] && (
            <Grid key={port} item xs={12}>
              <Card>
                <CardHeader
                  title={port}
                  action={
                    <Button variant="contained" href={`http://${port}.drt.homeoffice.gov.uk`}>View {port} arrivals</Button>
                  }
                />
                <CardContent>
                  <Alert severity="info">Pax exceed previous year at highlighted times</Alert>
                  <Line
                    id={port}
                    options={{
                      layout: {
                        padding: {
                          top: 10
                        }
                      },
                      plugins: {
                        legend: {
                          align: 'start',
                          title: {
                            padding: 20
                          },
                          labels: {
                            usePointStyle: true,
                          }
                        }
                      },
                      scales: {
                        x: {
                          border: {
                            display: true
                          },
                          type: 'time',
                          time: {
                            unit: timeUnits as "hour"
                          },
                          grid: {
                            display: true,
                            drawOnChartArea: true,
                            drawTicks: true
                          }
                        },
                        y: {
                          type: 'linear',
                          min: 0,
                          offset: true,
                          grace: '10%',
                          grid: {
                            display: true,
                          },
                        }
                      }
                    }}
                    plugins={[
                      {
                        id: "increase-legend-spacing",
                        beforeInit(chart) {
                          // Get reference to the original fit function
                          const originalFit = (chart.legend as any).fit;
                          // Override the fit function
                          (chart.legend as any).fit = function fit() {
                            // Call original function and bind scope in order to use `this` correctly inside it
                            originalFit.bind(chart.legend)();
                            this.height += 20;
                          };
                        }
                      }
                    ]}
                    data={{
                      datasets: [
                        {
                          label: `Pax:`,
                          backgroundColor: 'rgba(0, 94, 165, 0.2)',
                          borderColor: drtTheme.palette.primary.main,
                          borderDash: [5, 5],
                          borderWidth: 1,
                          pointStyle: 'rectRot',
                          fill: {
                            target: '+1',
                            above: 'rgba(0, 94, 165, 0.2)',
                            below: 'transparent',
                          },
                          data: portData[port].map((datapoint: TerminalDataPoint) => {
                            const pointDate = moment(datapoint.date)
                            if (interval === 'hour') {
                              pointDate.add(datapoint.hour, 'hours')
                            }
                            return {
                              x: pointDate.format('MM/DD/YYYY HH:mm'),
                              y: datapoint.totalPcpPax,
                            }
                          })
                        },
                        {
                          label: `Pax (previous year):`,
                          backgroundColor: 'transparent',
                          borderColor: '#547a00',
                          borderDash: [0, 0],
                          borderWidth: 1,
                          pointStyle: 'circle',
                          pointBackgroundColor: '#547a00',
                          data: historicPortData[port].map((datapoint: TerminalDataPoint) => {
                            const pointDate = moment(datapoint.date)
                            if (interval === 'hour') {
                              pointDate.add(datapoint.hour, 'hours')
                            }
                            return {
                              x: pointDate.add(1, 'year').format('MM/DD/YYYY HH:mm'),
                              y: datapoint.totalPcpPax,
                            }
                          })
                        }
                      ]
                    }}
                  />

                </CardContent>
              </Card>
            </Grid>
          )
        })
        }
      </Grid>
    </Box>
  )

}


const mapState = (state: RootState) => {
  return {
    errors: state.pressureDashboard?.errors,
    startDate: state.pressureDashboard?.start,
    endDate: state.pressureDashboard?.end,
    portData: state.pressureDashboard?.portData,
    historicPortData: state.pressureDashboard?.historicPortData,
    interval: state.pressureDashboard?.interval,
    type: state.pressureDashboard?.type,
  };
}

export default connect(mapState)(RegionalPressureDetail);
