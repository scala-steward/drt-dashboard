import axios from "axios";
import ApiClient from "../services/ApiClient";
import {User} from "../model/User";
import {useEffect, useState} from "react";


export const useUser = () => {
  const [user, setUser] = useState<User>({"kind": "PendingUser"})
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    const fetch = async () => axios
      .get(ApiClient.userEndPoint)
      .then(res => setUser({"kind": "SignedInUser", profile: res.data} as User))
      .catch(err => {
        console.log('Failed to get health check pauses: ' + err)
        setFailed(true)
      })
      .finally(() => setLoading(false))
    fetch()
  }, [fetch])

  return {user: user, loadingUser: loading, failedUser: failed}
}
