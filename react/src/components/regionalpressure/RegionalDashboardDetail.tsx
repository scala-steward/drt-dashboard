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
  Checkbox,
  useMediaQuery,
  Theme
} from "@mui/material";
import { Link } from 'react-router-dom';
import { ConfigValues } from "../../model/Config";
import { RootState } from '../../store/redux';
import drtTheme from '../../drtTheme';
import { Chart } from 'react-chartjs-2'; 
import {
  Chart as ChartJS,
  registerables
} from 'chart.js';
import 'chartjs-adapter-moment';
import moment from 'moment';
ChartJS.register(...registerables);
import { ArrowBack } from '@mui/icons-material';
import { TerminalDataPoint } from './regionalPressureSagas';
import RegionalPressureDates from './RegionalPressureDates';
import RegionalPressureForm from './RegionalPressureForm';
import RegionalPressureExport from './RegionalPressureExport';
import { getHistoricDateByDay } from './regionalPressureSagas';


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
  regionPorts.sort();

  const [visiblePorts, setVisiblePorts] = React.useState<string[]>([...regionPorts]);
  const availablePorts = config.ports.map(port => port.iata);
  const timeUnits = interval;

  const is_mobile = useMediaQuery((theme: Theme) => theme.breakpoints.down('md'));

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
        <Grid item xs={12} sm={10}>
          <FormControl component="fieldset" variant="standard">
            <FormLabel component="legend">Airports:</FormLabel>
            <FormGroup>
              <Stack direction={is_mobile ? 'column' : 'row'}>
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
        <Grid item xs={12} sm={2}>
          <Stack direction="row-reverse" spacing={2}>
            <RegionalPressureExport />
          </Stack>
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
                  <Alert severity="warning">Pax exceed previous year at highlighted times</Alert>
                  <Chart
                    type='line'
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
                          labels: {
                            usePointStyle: true,
                          }
                        },
                        tooltip: {
                          titleFont: {
                            size: 16
                          },
                          titleColor: (context) => {
                            if (context.tooltipItems.length > 1) {
                              let pax = context.tooltipItems[0].parsed.y
                              let historicPax = context.tooltipItems[1].parsed.y
                              let percentage = 100 * (pax - historicPax) / historicPax
                              if (percentage > 0) {
                                return '#f47738'
                              } else {
                                return '#c1d586'
                              }
                            }
                            return '#fff'
                          },
                          footerFont: {
                            size: 16
                          },
                          callbacks: {
                            title: function(context): string[] {
                              let formattedPaxPercent = ''
                              if (context.length > 1) {
                                let pax = context[0].parsed.y
                                let historicPax = context[1].parsed.y
                                let percentage = 100 * (pax - historicPax) / historicPax
                                formattedPaxPercent = new Intl.NumberFormat("en-US", {
                                  signDisplay: "exceptZero",
                                  maximumSignificantDigits: 2
                                
                                }).format(percentage);
                              }
                              return [`${formattedPaxPercent}% Pax expected`]
                            },
                            label: function(context) : string {
                              let date = moment(context.parsed.x)
                              let dateFormat = type == 'single' ? 'HH:mm ddd Do MMM YYYY ' : 'ddd Do MMM YYYY'
                              switch (context.dataset.label) {
                                case 'Pax arrivals':
                                  return `${date.format(dateFormat)}: ${context.parsed.y}`
                                case 'Previous year':
                                  return `${getHistoricDateByDay(date).format(dateFormat)}: ${context.parsed.y}`
                                default: 
                                  return ''
                              }
                            }
                          }
                        }
                      },
                      interaction: {
                        mode: 'nearest',
                        axis: 'x',
                        intersect: false,
                      },
                      scales: {
                        x: {
                          position: 'bottom',
                          border: {
                            display: false
                          },
                          type: 'time',
                          time: {
                            unit: timeUnits as "hour"
                          },
                          grid: {
                            display: true,
                            drawOnChartArea: true,
                            drawTicks: true
                          },
                          offset: true,
                          ticks: {
                            callback: (label) => {
                              let date = moment(label)
                              return type == 'single' ? date.format('HH:mm') : date.format('ddd Do MMM')
                            }
                          }
                        },
                        y: {
                          type: 'linear',
                          min: 0,
                          grace: '10%',
                          grid: {
                            display: true,
                          },
                        },
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
                          label: `Pax arrivals`,
                          type: 'line',
                          backgroundColor: 'rgba(0, 94, 165, 0.2)',
                          borderColor: '#005ea5',
                          borderDash: [0, 0],
                          borderWidth: 1,
                          pointStyle: 'rectRot',
                          pointRadius: 5,
                          pointHoverRadius: 10,
                          pointBackgroundColor: '#005ea5',
                          xAxisID: 'x',
                          fill: {
                            target: '1',
                            above: 'rgba(255, 244, 229, 0.7)',
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
                          label: `Previous year`,
                          type: 'line',
                          borderColor: drtTheme.palette.grey[800],
                          borderDash: [5, 5],
                          borderWidth: 1,
                          pointStyle: 'circle',
                          pointRadius: 3,
                          pointHoverRadius: 10,
                          data: historicPortData[port].map((datapoint: TerminalDataPoint, index: number) => {
                            const paxDate = moment(portData[port][index].date)
                            const pointDate = moment(datapoint.date)
                            let historicDayOffset = 0;
                            if (interval === 'hour') {
                              pointDate.set('date', paxDate.date())
                              pointDate.add(datapoint.hour, 'hours')
                            } else {
                              historicDayOffset =  moment.duration(pointDate.diff(moment(paxDate).subtract(1,'y'))).asDays();
                            }
                            return {
                              x: pointDate.add(1, 'year').subtract(historicDayOffset, 'days').format('MM/DD/YYYY HH:mm'),
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
