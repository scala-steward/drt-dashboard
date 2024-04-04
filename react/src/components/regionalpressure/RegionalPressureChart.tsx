import * as React from 'react';
import {connect} from 'react-redux';
import {
  Alert,
  Card,
  CardContent,
  CardHeader,
  Button,
  Stack,
  useTheme,
  Box,
} from "@mui/material";
import { Link } from 'react-router-dom';
import { CheckCircle } from '@mui/icons-material';
import ErrorIcon from '@mui/icons-material/Error';
import {RootState} from '../../store/redux';
import drtTheme from '../../drtTheme';
import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
  TooltipItem,
  ChartType,
} from 'chart.js';
import { Radar } from 'react-chartjs-2';

ChartJS.register(
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend
);

interface RegionalPressureChartProps {
  regionName: string;
  portCodes: string[];
  portTotals: {
    [key: string]: number
  };
  historicPortTotals: {
    [key: string]: number
  };
}

const doesExceed = (forecast: number, historic: number): boolean => {
  return forecast > historic;
}

const RegionalPressureChart = ({regionName, portCodes, portTotals, historicPortTotals}: RegionalPressureChartProps) => {
  const theme = useTheme();

  const forecasts = [...portCodes].map((portCode) => {
    return (portTotals[portCode] - historicPortTotals[portCode]) / (historicPortTotals[portCode]) * 100
  })
  const historics = [...portCodes].map((portCode) => {
    return historicPortTotals[portCode]
  })
  const historic_zero = [...portCodes].map(() => 0);

  const chartData = {
    labels: portCodes,
    datasets: [
      {
        label: 'Forecasted pax arrivals',
        data: forecasts,
        backgroundColor: 'rgba(0, 94, 165, 0.2)',
        borderColor: drtTheme.palette.primary.main,
        borderDash: [5, 5],
        pointStyle: 'rectRot',
        borderWidth: 1,
        tooltip: {
          callbacks: {
              label: function(context: TooltipItem<ChartType>) {
                const port = context.label;
                const arrivals = portTotals[port];
                const value = new Intl.NumberFormat("en-US", {
                    style: 'decimal',
                    signDisplay: "exceptZero"
                }).format(context.parsed.r);
                return `${arrivals} arrivals (${value}%)`
              }
          }
        },
      },
      {
        label: 'Historic pax average',
        data: historic_zero,
        backgroundColor: 'transparent',
        borderColor: '#547a00',
        pointStyle: 'circle',
        pointBackgroundColor: '#547a00',
        borderDash: [0,0],
        borderWidth: 1,
        tooltip: {
          callbacks: {
              label: function(context: TooltipItem<ChartType>) {
                const port = context.label;
                const arrivals = historicPortTotals[port];
                return `${arrivals} arrivals`
              }
          }
        },
      },
    ],
  };

  const exceededForecasts = forecasts.map((forecast, index) => {return (doesExceed(forecast!, historics[index]!))});
  const exceededCount = exceededForecasts.filter(forecast => forecast).length;

  const chartOptions = {
    layout: {
      padding: 0,
    },
    plugins: {
      datalabels: {
          formatter: (value: number) => {
              return `${value}%`;
          },
          color: '#fff',
      },
      legend: {
        labels: {
          usePointStyle: true,
        }
      }
    },
    scales: {
      r: {
        grid: {
          color: [theme.palette.grey[300]],
        },
        suggestedMin: -100,
        suggestedMax: 100,
        ticks: {
          callback: ((tick: any) => {
            return `${tick}%`
          }),
          backdropColor: theme.palette.grey[700],
          color: '#fff'
        },
        pointLabels: {
          callback: (label: string, index: number): string | number | string[] | number[] => {
            return doesExceed(forecasts[index]!, historics[index]!) ? `ⓘ ${label}` : `${label}`;
          },
          font: {
            weight: (context: any) => {
              return doesExceed(forecasts[context.index]!, historics[context.index]!) ? 'bold' : 'normal'
            }
          },
          color: (context: any) => {
            return doesExceed(forecasts[context.index]!, historics[context.index]!) ? theme.palette.info.main : 'black';
          },
        },
      }
    }
  }

  return (
    <Card variant='outlined'>
      <CardHeader component='h3' sx={{m: 0, textAlign: 'center'}} title={regionName !== 'Heathrow' ? `${regionName} Region` : regionName} />
      <CardContent sx={{px: 0}}>
        <Stack sx={{ width: '100%' }} spacing={2}>
          <Radar data={chartData} options={chartOptions} />
          <Box sx={{px: 2}}>
            { exceededCount > 0 ?
                <Alert icon={<ErrorIcon fontSize="inherit" />} severity="info">
                  {`Pax number exceeds previous year at ${exceededCount} airports`}
                </Alert>
              :
                <Alert icon={<CheckCircle fontSize="inherit" />} severity="success">
                  Pax number does not exceed previous year at any airport
                </Alert>
            }
            <Button component={Link} to={`${regionName.toLowerCase()}`} fullWidth variant='contained' sx={{mt:2}}>
              {regionName !== 'Heathrow' ? `View ${regionName} region` : `View ${regionName}`}
            </Button>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
  
}


const mapState = (state: RootState) => {
  return { 
    portTotals: state.pressureDashboard?.portTotals,
    historicPortTotals: state.pressureDashboard?.historicPortTotals,
   };
}


export default connect(mapState)(RegionalPressureChart);
