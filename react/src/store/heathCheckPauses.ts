import axios from "axios";
import ApiClient from "../services/ApiClient";
import {ScheduledHealthCheckPause} from "../components/healthcheckpauseseditor/model";
import {useEffect, useState} from "react";


export type RequestSetHealthCheckPauses = {
  pause: ScheduledHealthCheckPause
  onSuccess: () => void
  onFailure: () => void
}

export type RequestDeleteHealthCheckPauses = {
  startsAt: number
  endsAt: number
  onSuccess: () => void
  onFailure: () => void
}

export const deleteHealthCheckPause = (request: RequestDeleteHealthCheckPauses) => {
  axios
    .delete(`${ApiClient.healthCheckPauses}/${request.startsAt}/${request.endsAt}`)
    .then(() => request.onSuccess())
    .catch(reason => {
      console.log('Failed to delete red list updates: ' + reason)
      request.onFailure()
    })
}

export const saveHealthCheckPauses = (request: RequestSetHealthCheckPauses) => {
  axios
    .post(ApiClient.healthCheckPauses, request.pause)
    .then(() => request.onSuccess())
    .catch(reason => {
      console.log('Failed to save red list updates: ' + reason)
      request.onFailure()
    })
}

export const useHealthCheckPauses = (refreshedAt: number) => {
  const [healthCheckPauses, setHealthCheckPauses] = useState<ScheduledHealthCheckPause[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const fetch = async () => axios
      .get(ApiClient.healthCheckPauses)
      .then(res => setHealthCheckPauses(res.data as ScheduledHealthCheckPause[]))
      .catch(err => {
        console.log('Failed to get health check pauses: ' + err)
        setFailed(true)
      })
      .finally(() => setLoading(false))
    fetch()
  }, [fetch, refreshedAt])

  return {healthCheckPauses, loading, failed}
}
