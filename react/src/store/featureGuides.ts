import axios from "axios";
import {enqueueSnackbar} from "notistack";
import {useEffect, useState} from "react";
import {FeatureGuide} from "../components/featureguides/FeatureGuidesList";
import ApiClient from "../services/ApiClient";

export const deleteFeatureGuide = (id: string) => {
  axios.delete(`${ApiClient.featureGuidesEndpoint}/${id}`, )
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
  axios.post(`${ApiClient.featureGuidesUpdatePublishedEndpoint}/${id}`, {published: publish})
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
  const [features, setFeatures] = useState<FeatureGuide[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const fetch = () => axios.get(ApiClient.featureGuidesEndpoint)
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
