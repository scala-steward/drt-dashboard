import React, {useEffect, useState} from 'react'
import {Box, Collapse, IconButton, TableHead, Tooltip, Typography} from "@mui/material"
import {HealthCheck, PortHealthCheckAlarms, useHealthCheckAlarms, useHealthChecks} from "../../store/heathChecks";
import {Moment} from "moment"
import moment from "moment-timezone"
import {PortRegion} from "../../model/Config";
import TableContainer from "@mui/material/TableContainer";
import Paper from "@mui/material/Paper";
import Table from "@mui/material/Table";
import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import TableBody from "@mui/material/TableBody";
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import {Helmet} from "react-helmet";
import {adminPageTitleSuffix} from "../../utils/common";

interface Props {
  portsByRegion: PortRegion[]
}

export const HealthChecks = (props: Props) => {

  const [requestRefreshAt, setRequestRefreshAt] = useState<Moment>(moment())
  const {healthCheckAlarms, loading, failed} = useHealthCheckAlarms(requestRefreshAt.valueOf())
  const healthChecks = useHealthChecks()

  useEffect(() => {
    const set = () => setInterval(() => {
      console.log('refreshing health checks')
      setRequestRefreshAt(moment())
    }, 5000)
    set()
  }, []);

  return <>
    <Helmet>
      <title>Health Checks {adminPageTitleSuffix}</title>
    </Helmet>
    <Box>
      <h1>Health Checks</h1>
      {loading && <p>Loading...</p>}
      {failed && <p>Failed to load health checks</p>}

      <TableContainer component={Paper}>
        <Table aria-label="collapsible table">
          <TableHead>
            <TableRow>
              <TableCell/>
              <TableCell>
                <Typography variant={'h5'}>Region</Typography>
              </TableCell>
              {healthChecks.healthChecks.map((healthCheck, index) => {
                return <TableCell key={index} align="right">
                  <Tooltip title={healthCheck.description} placement={'top'} arrow>
                    <Typography variant={'h5'}>{healthCheck.name}</Typography>
                  </Tooltip>
                </TableCell>
              })}
            </TableRow>
          </TableHead>
          <TableBody>
            {props.portsByRegion.map((portRegion, index) => {
              const regionAlarms = portRegion.ports
                .filter(port => healthCheckAlarms.some(alarm => alarm.port === port))
                .sort((a, b) => a.localeCompare(b))
                .map(port => healthCheckAlarms.find(alarm => alarm.port === port))
                .filter(alarm => !!alarm) as PortHealthCheckAlarms[]

              if (regionAlarms.length !== 0 && regionAlarms)
                return <Row region={portRegion.name} regionPortAlarms={regionAlarms}
                            healthChecks={healthChecks.healthChecks} key={index}/>
              else
                return <></>
            })}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  </>
}

function Row(props: { region: string, regionPortAlarms: PortHealthCheckAlarms[], healthChecks: HealthCheck[] }) {
  const [open, setOpen] = React.useState(false);

  const regionAlarms = props.healthChecks.map(healthCheck => {
    return props.regionPortAlarms.findIndex(portAlarms => {
      const healthCheckAlarmForPortIsActive = portAlarms.alarms
        .findIndex(portAlarm => portAlarm.name === healthCheck.name && portAlarm.isActive) !== -1
      return healthCheckAlarmForPortIsActive
    }) !== -1
  })

  const regionCellWidth = `${75 / props.healthChecks.length}%`
  const portCellWidth = `${84 / props.healthChecks.length}%`

  return (
    <React.Fragment>
      <TableRow sx={{'& > *': {borderBottom: 'unset'}}}>
        <TableCell width={88}>
          <IconButton
            aria-label="expand row"
            size="small"
            onClick={() => setOpen(!open)}
          >
            {open ? <KeyboardArrowUpIcon/> : <KeyboardArrowDownIcon/>}
          </IconButton>
        </TableCell>
        <TableCell scope="row">
          <Typography variant={'subtitle2'}>{props.region}</Typography>
        </TableCell>
        {regionAlarms.map((alarmIsActive, index) => {
          return <TableCell width={regionCellWidth} key={index} align={'right'}>
            {alarmIsActive ? <ErrorOutlineRoundedIcon color={'error'} fontSize={'medium'}/> :
              <CheckCircleOutlineRoundedIcon color={'success'} fontSize={'medium'}/>}
          </TableCell>
        })}
      </TableRow>
      <TableRow>
        <TableCell style={{paddingLeft: 120, paddingTop: 0, paddingBottom: 0, paddingRight: 0}} colSpan={6}>
          <Collapse in={open} timeout="auto" unmountOnExit>
            <Table aria-label="collapsible table">
              <TableBody>
                {props.regionPortAlarms.map((portAlarms, index) => {
                  return portAlarms ? <TableRow key={index} sx={{backgroundColor: '#fafafa'}}>
                      <TableCell><Typography variant={'body1'}>{portAlarms.port}</Typography></TableCell>
                      {portAlarms.alarms.map((alarm, index) => {
                        return <TableCell width={portCellWidth} key={index} align={'right'}>
                          {alarm.isActive ? <ErrorOutlineRoundedIcon color={'error'}/> :
                            <CheckCircleOutlineRoundedIcon color={'success'}/>}
                        </TableCell>
                      })}
                    </TableRow> :
                    <></>
                })}
              </TableBody>
            </Table>
          </Collapse>
        </TableCell>
      </TableRow>
    </React.Fragment>
  );
}
