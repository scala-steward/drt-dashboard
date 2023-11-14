import axios, {AxiosResponse} from "axios"


interface IApiClient {
  fetchData: (userEndPoint: string, handleResponse: (r: AxiosResponse) => void) => void
}

export default class ApiClient implements IApiClient {
  public static userEndPoint = "/api/user"
  public static userTrackingEndPoint = "/api/track-user"
  public static configEndPoint = "/api/config"
  public static logoutEndPoint = "/oauth/logout"
  public static alertsEndPoint = "/api/alerts"
  public static healthCheckPauses = "/api/health-check-pauses"

  public static userDetailsEndpoint = "/api/users/user-details"
  public static requestAccessEndPoint = "/api/users/access-request"
  public static addUserToGroupEndpoint = "/api/users/accept-access-request"
  public static updateUserRequestEndpoint = "/api/users/update-access-request"
  public static userListEndpoint = "/api/users/all"

  public static getDropInSessionEndpoint = "/api/drop-in-sessions"
  public static dropInSessionUpdatePublishedEndpoint = "/api/drop-in-sessions/update-published"
  public static dropInSessionDeletePublishedEndpoint = "/api/drop-in-sessions"
  public static dropInSessionRegistrationsEndpoint = "/api/drop-in-register"
  public static dropInSessionRegistrationDeleteEndpoint = "/api/drop-in-register"

  public static featureGuidesEndpoint = "/api/feature-guides"
  public static featureGuidesVideosEndpoint = "/api/feature-guides/get-feature-videos"
  public static featureGuidesUpdatePublishedEndpoint = "/api/feature-guides/update-published"


  public fetchData(userEndPoint: string, handleResponse: (r: AxiosResponse) => void) {
    axios
      .get(userEndPoint)
      .then(response => handleResponse(response))
      .catch(t => this.handleAjaxException(userEndPoint, t))
  }

  public sendData(userEndPoint: string, data: any, handleResponse: (r: AxiosResponse) => void) {
    axios
      .post("/user/access-request", data)
      .then(r => handleResponse(r))
      .catch(t => console.log('caught: ' + t))
  }

  handleAjaxException(endPoint: string, throwable: any) {
    console.log('caught: ' + throwable)
  }

}
