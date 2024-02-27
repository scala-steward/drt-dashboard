import React, {useEffect, useState} from 'react'
import {Box, Chip, Stack} from "@mui/material"
import {useHealthCheckAlarms} from "../../store/heathChecks";
import {Moment} from "moment"
import moment from "moment-timezone"
import {PortRegion} from "../../model/Config";

interface Props {
  portsByRegion: PortRegion[]
}

export const HealthChecks = (props: Props) => {

  const [requestRefreshAt, setRequestRefreshAt] = useState<Moment>(moment())
  const {healthCheckAlarms, loading, failed} = useHealthCheckAlarms(requestRefreshAt.valueOf())

  useEffect(() => {
    const set = () => setInterval(() => {
      console.log('refreshing health checks')
      setRequestRefreshAt(moment())
    }, 5000)
    set()
  }, []);

  return <Box>
    <h1>Health Checks</h1>
    {loading && <p>Loading...</p>}
    {failed && <p>Failed to load health checks</p>}
    <Stack>
      {props.portsByRegion.map((portRegion, index) => {
        const regionAlarms = portRegion.ports
          .filter(port => healthCheckAlarms.some(alarm => alarm.port === port))
          .sort((a, b) => a.localeCompare(b))
          .map(port => healthCheckAlarms.find(alarm => alarm.port === port))

        if (regionAlarms.length === 0)
          return <></>
        else
          return <Box key={index}>
            <h2>{portRegion.name}</h2>
            <Stack direction={'column'} gap={2}>
            {regionAlarms.map((portAlarms, index) => {
              return portAlarms ? <Stack direction={'row'} key={index} gap={2}>
                  <Box minWidth={'100px'}>{portAlarms.port}</Box>

                  {portAlarms.alarms.map((alarm, index) => {
                    return <Chip key={index} color={alarm.isActive ? 'warning' : 'success'} label={alarm.name}/>
                  })}

                </Stack> :
                <></>
            })}
            </Stack>
          </Box>
      })}
    </Stack>

  </Box>
}
