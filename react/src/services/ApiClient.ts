import axios, {AxiosResponse} from "axios"


interface IApiClient {
  fetchData: (userEndPoint: string, handleResponse: (r: AxiosResponse) => void) => void
}

export default class ApiClient implements IApiClient {
  public static userEndPoint = "/api/user"
  public static userTrackingEndPoint = "/api/track-user"
  public static configEndPoint = "/api/config"
  public static logoutEndPoint = "/oauth2/sign_out"
  public static alertsEndPoint = "/api/alerts"
  public static healthChecks = "/api/health-checks"
  public static healthCheckAlarmStatuses = "/api/health-checks/alarm-statuses"
  public static healthCheckPauses = "/api/health-check-pauses"

  public static userDetailsEndpoint = "/api/users/user-details"
  public static requestAccessEndPoint = "/api/users/access-request"
  public static addUserToGroupEndpoint = "/api/users/accept-access-request"
  public static updateUserRequestEndpoint = "/api/users/update-access-request"
  public static userListEndpoint = "/api/users/all"

  public static getDropInSessionEndpoint = "/api/drop-in-sessions"
  public static dropInSessionUpdatePublishedEndpoint = "/api/drop-in-sessions/update-published"
  public static dropInSessionRegistrationsEndpoint = "/api/drop-in-register"
  public static dropInSessionRegistrationDeleteEndpoint = "/api/drop-in-register"

  public static featureGuidesEndpoint = "/api/feature-guides"
  public static featureGuidesFileEndpoint = "/api/feature-guides/get-feature-file"
  public static featureGuidesUpdatePublishedEndpoint = "/api/feature-guides/update-published"

  public static exportRegionEndpoint = "/api/export-region"
  public static exportEndpoint = "/api/export"
  public static exportStatusEndpoint = "/api/export/status"

  public static feedbackEndpoint = "/api/feedback"

  public static exportConfigEndpoint = "/api/export-config"

  public static passengerTotalsEndpoint = "/api/passengers/"

  public static borderCrossingEndpoint = "/api/border-crossing"


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
