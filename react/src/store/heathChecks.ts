import axios from "axios";
import ApiClient from "../services/ApiClient";
import {useEffect, useState} from "react";

export interface HealthCheck {
  name: string,
  description: string,
  priority: string,
}

export interface HealthCheckAlarm {
  name: string,
  isActive: boolean,
}

export interface PortHealthCheckAlarms {
  port: string,
  alarms: HealthCheckAlarm[]
}

export const useHealthCheckAlarms = (refreshedAt: number) => {
  const [healthCheckAlarms, setHealthCheckAlarms] = useState<PortHealthCheckAlarms[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const fetch = async () => axios
      .get(ApiClient.healthCheckAlarmStatuses)
      .then(res => setHealthCheckAlarms(res.data as PortHealthCheckAlarms[]))
      .catch(err => {
        console.log('Failed to get health check alarm statuses: ' + err)
        setFailed(true)
      })
      .finally(() => setLoading(false))
    fetch()
  }, [fetch, refreshedAt])

  return {healthCheckAlarms, loading, failed}
}


export const useHealthChecks = () => {
  const [healthChecks, setHealthCheckAlarms] = useState<HealthCheck[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const fetch = async () => axios
      .get(ApiClient.healthChecks)
      .then(res => setHealthCheckAlarms(res.data as HealthCheck[]))
      .catch(err => {
        console.log('Failed to get health checks: ' + err)
        setFailed(true)
      })
      .finally(() => setLoading(false))
    fetch()
  }, [fetch])

  return {healthChecks, loading, failed}
}
