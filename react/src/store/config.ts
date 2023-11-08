import axios from "axios";
import ApiClient from "../services/ApiClient";
import {Config, ConfigValues} from "../model/Config";
import {useEffect, useState} from "react";


export const useConfig = () => {
  const [config, setConfig] = useState<Config>({"kind": "PendingConfig"})
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const fetch = async () => axios
      .get(ApiClient.configEndPoint)
      .then(res => setConfig({kind: "LoadedConfig", values: res.data as ConfigValues}))
      .catch(err => {
        console.log('Failed to get health check pauses: ' + err)
        setFailed(true)
      })
      .finally(() => setLoading(false))
    fetch()
  }, [fetch])

  return {config: config, loadingConfig: loading, failedConfig: failed}
}
