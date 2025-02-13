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
  forecastTotals: {
    [key: string]: number
  };
  historicTotals: {
    [key: string]: number
  };
}

const doesExceed = (forecast: number): boolean => {
  return forecast > 0
}

const RegionalPressureChart = ({regionName, portCodes, forecastTotals, historicTotals}: RegionalPressureChartProps) => {
  const theme = useTheme();

  const forecasts = [...portCodes].map((portCode) => {
    return (forecastTotals[portCode] - historicTotals[portCode]) / (historicTotals[portCode]) * 100
  })
  const historic_zero = [...portCodes].map(() => 0);

  const chartData = {
    labels: portCodes,
    datasets: [
      {
        label: 'Forecast pax arrivals',
        data: forecasts,
        backgroundColor: 'transparent',
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
        tooltip: {
          callbacks: {
              label: function(context: TooltipItem<ChartType>) {
                const port = context.label;
                const arrivals = forecastTotals[port];
                const value = new Intl.NumberFormat("en-US", {
                    style: 'decimal',
                    signDisplay: "exceptZero",
                    maximumFractionDigits: 0
                }).format(context.parsed.r);
                return `${arrivals.toLocaleString()} pax (${value}%)`
              }
          }
        },
      },
      {
        label: 'Previous year',
        data: historic_zero,
        backgroundColor: 'transparent',
        borderColor: drtTheme.palette.grey[800],
        borderDash: [5, 5],
        borderWidth: 2,
        pointStyle: 'circle',
        pointRadius: 5,
        pointHoverRadius: 8,
        pointHoverBorderWidth: 3,
        pointBackgroundColor: '#ffffff',
        pointHoverBackgroundColor: '#ffffff',
        tooltip: {
          callbacks: {
              label: function(context: TooltipItem<ChartType>) {
                const port = context.label;
                const arrivals = historicTotals[port];
                return `${arrivals.toLocaleString()} historical pax`
              }
          }
        },
      },
    ],
  };

  const exceededForecasts = forecasts.map(doesExceed);
  const exceededCount = exceededForecasts.filter(forecast => forecast).length;

  const chartOptions = {
    layout: {
      padding: 10,
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
            return doesExceed(forecasts[index]) ? `ⓘ ${label}` : `${label}`;
          },
          font: {
            weight: (context: any) => {
              return doesExceed(forecasts[context.index]) ? 'bold' : 'normal'
            }
          },
          color: (context: any) => {
            return doesExceed(forecasts[context.index]) ? theme.palette.info.main : 'black';
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
    forecastTotals: state.pressureDashboard?.forecastTotals,
    historicTotals: state.pressureDashboard?.historicTotals,
    forecastStart: state.pressureDashboard?.forecastStart,
    forecastEnd: state.pressureDashboard?.forecastEnd,
    historicStart: state.pressureDashboard?.historicStart,
    historicEnd: state.pressureDashboard?.historicEnd,
   };
}


export default connect(mapState)(RegionalPressureChart);
