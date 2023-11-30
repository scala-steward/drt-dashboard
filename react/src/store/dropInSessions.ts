import {useEffect, useState} from "react";
import axios from "axios";
import ApiClient from "../services/ApiClient";

export interface DropInSession {
  id: number,
  title: string,
  startTime: string,
  endTime: string,
  meetingLink: string,
  lastUpdatedAt: string,
  isPublished: boolean,
}

export const useDropInSessions = (showAll: boolean, refreshedAt: number) => {
  const [dropInSessions, setDropInSessions] = useState<DropInSession[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const fetch = async () => axios
      .get(`${ApiClient.getDropInSessionEndpoint}?list-all=${showAll}`)
      .then(res => setDropInSessions(res.data as DropInSession[]))
      .catch(err => {
        console.log('Failed to get health check pauses: ' + err)
        setFailed(true)
      })
      .finally(() => setLoading(false))
    fetch()
  }, [showAll, refreshedAt])

  return {dropInSessions, loading, failed}
}
