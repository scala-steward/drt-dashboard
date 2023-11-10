import axios from "axios";
import {enqueueSnackbar} from "notistack";
import {useEffect, useState} from "react";
import {ScheduledHealthCheckPause} from "../components/healthcheckpauseseditor/model";

export const deleteFeatureGuide = (id: string) => {
  axios.delete(`/guide/removeFeatureGuide/${id}`, )
    .then(response => {
      if (response.status === 200)
        enqueueSnackbar('Feature guide deleted successfully', {variant: 'success'})
      else {
        enqueueSnackbar('Failed to delete feature guide', {variant: 'error'})
      }
    })
    .catch(() =>
      enqueueSnackbar('Failed to delete feature guide', {variant: 'error'})
    )
}

export const updatePublishedStatus = (id: string, publish: boolean, onComplete: () => void) => {
  axios.post(`/guide/published/${id}`, {published: publish})
    .then(response => {
      if (response.status === 200)
        enqueueSnackbar('Feature guide ' + (publish ? 'published' : 'unpublished') + ' successfully', {variant: 'success'})
      else {
        enqueueSnackbar((publish ? 'Publishing' : 'Unpublishing') + ' the feature guide failed', {variant: 'error'})
      }
    })
    .catch(() =>
      enqueueSnackbar((publish ? 'Publishing' : 'Unpublishing') + ' the feature guide failed', {variant: 'error'})
    )
    .finally(() => onComplete())
}

export const useFeatureGuides = (refreshedAt: number) => {
  const [features, setFeatures] = useState<ScheduledHealthCheckPause[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const fetch = () => axios.get('/guide/getFeatureGuides')
      .then(response => {
        if (response.status === 200) {
          setFeatures(response.data)
        } else {
          setFailed(true)
        }
      })
      .catch(err => {
        console.log('Failed to get health check pauses: ' + err)
        setFailed(true)
      })
      .finally(() => setLoading(false))
    fetch()
  }, [refreshedAt])

  return {features: features, loading, failed}
}
