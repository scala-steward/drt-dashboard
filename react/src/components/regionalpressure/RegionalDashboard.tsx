import * as React from 'react'
import { connect } from 'react-redux'
import { UserProfile } from "../../model/User"
import { useParams } from 'react-router'
import pattern from 'patternomaly'
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
} from "@mui/material"
import { Link } from 'react-router-dom'
import { ConfigValues } from "../../model/Config"
import { RootState } from '../../store/redux'
import drtTheme from '../../drtTheme'
import { Chart } from 'react-chartjs-2'
import {
  Chart as ChartJS,
  registerables,
} from 'chart.js'
import 'chartjs-adapter-moment'
import moment from 'moment'
ChartJS.register(...registerables)
import { ArrowBack } from '@mui/icons-material'
import { TerminalDataPoint } from './regionalPressureSagas'
import RegionalPressureDates from './RegionalPressureDates'
import RegionalPressureForm from './RegionalPressureForm'
import RegionalPressureExport from './RegionalPressureExport'


interface RegionalDashboardProps {
  config: ConfigValues
  user: UserProfile
  title?: string
  interval?: string
  forecastData: {
    [key: string]: TerminalDataPoint[]
  }
  historicData: {
    [key: string]: TerminalDataPoint[]
  }
}

const RegionalDashboard = ({ config, forecastData, historicData, interval }: RegionalDashboardProps) => {
  const { region } = useParams() || ''
  let regionPorts = config.portsByRegion.filter((r) => r.name.toLowerCase() === region!.toLowerCase())[0].ports
  let title = `${region} Region`
  let portLabel = 'Airports:'
  if (region === 'heathrow') {
    title = 'Heathrow'
    portLabel == 'Aiport terminals:'
    regionPorts = ['LHR-T2', 'LHR-T3', 'LHR-T4', 'LHR-T5']
  }
  regionPorts.sort()

  const [visiblePorts, setVisiblePorts] = React.useState<string[]>([...regionPorts])
  const availablePorts = config.ports.map(port => port.iata)
  const timeUnits = interval

  const is_mobile = useMediaQuery((theme: Theme) => theme.breakpoints.down('md'))

  const handleTogglePort = (port: string) => {
    if (visiblePorts.includes(port)) {
      const newPorts = [...visiblePorts]
      newPorts.splice(visiblePorts.indexOf(port), 1)
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
          <h2 style={{ textTransform: 'capitalize' }}>{ title }</h2>
        </Grid>
      </Grid>
      <Grid>
        <RegionalPressureForm ports={regionPorts} availablePorts={availablePorts} />
      </Grid>
      <Grid container spacing={2} justifyItems={'stretch'} sx={{ mb: 2 }}>
        <Grid item xs={10}>
          <RegionalPressureDates />
        </Grid>
        <Grid item xs={12} sm={8}>
          <FormControl component="fieldset" variant="standard">
            <FormLabel component="legend">{ portLabel }</FormLabel>
            <FormGroup>
              <Stack direction={is_mobile ? 'column' : 'row'}>
                {
                  regionPorts && regionPorts.map((port: string) => {
                    return <FormControlLabel
                      key={port}
                      value={port}
                      control={<Checkbox checked={visiblePorts.includes(port)} value={port} />}
                      onClick={() => handleTogglePort(port)}
                      label={port.toUpperCase().replace("-", ' ')}
                      labelPlacement="end"
                    />
                  })
                }
              </Stack>
            </FormGroup>
          </FormControl>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Stack direction="row-reverse" spacing={2}>
            <RegionalPressureExport />
          </Stack>
        </Grid>
        {regionPorts && regionPorts.map((port: string) => {
          const portName = port.replace("-", ' ')
          return visiblePorts.includes(port) && forecastData[port] && (
            <Grid key={port} item xs={6}>
              <Card>
                <CardHeader
                  title={portName}
                  action={
                    <Button variant="contained" href={`http://${port}.drt.homeoffice.gov.uk`}>View {portName} arrivals</Button>
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
                                return '#FFB3BA'
                              } else {
                                return '#C3E072'
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

                                }).format(percentage)
                                percentage = isNaN(percentage) ? 0 : percentage
                              }
                              return [`${formattedPaxPercent}% pax expected at ${port}`]
                            },
                            label: function(context) : string[] {
                              let date = moment(context.parsed.x)
                              let dateFormat = timeUnits == 'hour' ? 'h:mm a ddd D MMM YYYY ' : 'ddd D MMM YYYY'
                              const historicDate = moment(historicData[port][context.dataIndex]?.date) || moment()
                              if (interval == 'hour') {
                                historicDate.add(context.dataIndex, 'hours')
                              }
                              switch (context.dataset.label) {
                                case 'Pax arrivals':
                                  return [` ${date.format(dateFormat)}: ${context.parsed.y.toLocaleString()} pax`]
                                case 'Historical pax':
                                  return [` ${historicDate.format(dateFormat)}: ${context.parsed.y.toLocaleString()} pax`]
                                default:
                                  return [` ${date.format(dateFormat)}: ${context.parsed.y.toLocaleString()} pax`]
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
                              return timeUnits == 'hour' ? date.format('HH:mm') : date.format('ddd D MMM')
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
                            originalFit.bind(chart.legend)()
                            this.height += 20
                          }
                        }
                      }
                    ]}
                    data={{
                      datasets: [
                        {
                          label: `Pax arrivals`,
                          type: 'line',
                          backgroundColor: [
                            pattern.draw('diagonal', '#C94900'),
                          ],
                          borderColor: '#005ea5',
                          borderDash: [0, 0],
                          borderWidth: 4,
                          pointStyle: 'rectRot',
                          pointRadius: 10,
                          pointHoverRadius: 12,
                          pointBackgroundColor: '#005ea5',
                          pointBorderColor: '#ffffff',
                          pointBorderWidth: 3,
                          pointHoverBorderWidth: 3,
                          pointHoverBorderColor: '#ffffff',
                          pointHoverBackgroundColor: '#0E2560',
                          xAxisID: 'x',
                          fill: {
                            target: '1',
                            below: 'transparent',
                          },
                          data: forecastData[port].map((datapoint: TerminalDataPoint) => {
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
                          label: `Historical pax`,
                          type: 'line',
                          borderColor: drtTheme.palette.grey[800],
                          borderDash: [5, 5],
                          borderWidth: 2,
                          pointStyle: 'circle',
                          pointRadius: 5,
                          pointHoverRadius: 8,
                          pointHoverBorderWidth: 3,
                          pointBackgroundColor: '#ffffff',
                          pointHoverBackgroundColor: '#ffffff',
                          data: historicData[port].map((datapoint: TerminalDataPoint, index: number) => {
                              const paxDate = moment(forecastData[port][index]?.date) || moment()
                              if (interval === 'hour') {
                                paxDate.add(datapoint.hour, 'hours')
                              }
                              return {
                                x: paxDate.format('MM/DD/YYYY HH:mm'),
                                y: datapoint.totalPcpPax,
                              }
                          }),
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
    forecastData: state.pressureDashboard?.forecastData,
    historicData: state.pressureDashboard?.historicData,
    interval: state.pressureDashboard?.interval,
  }
}

export default connect(mapState)(RegionalDashboard)
