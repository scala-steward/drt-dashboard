import axios, {AxiosResponse} from "axios";


interface IApiClient {
  fetchData: (userEndPoint: string, handleResponse: (r: AxiosResponse) => void) => void
}

export default class ApiClient implements IApiClient {
  public static userEndPoint = "/api/user";
  public static configEndPoint = "/api/config";
  public static requestAccessEndPoint = "/api/access-request";
  public static logoutEndPoint = "/oauth/logout";
  public static alertsEndPoint = "/api/alerts";
  public static neboFileUpload = "/api/nebo-upload";
  public static redListUpdates = "/api/red-list-updates";
  public static userDetailsEndpoint = "/api/user-details";
  public static addUserToGroupEndpoint = "/api/accept-user-request";

  public fetchData(userEndPoint: string, handleResponse: (r: AxiosResponse) => void) {
    axios
      .get(userEndPoint)
      .then(response => handleResponse(response))
      .catch(t => this.handleAjaxException(userEndPoint, t))
  }

  public sendData(userEndPoint: string, data: any, handleResponse: (r: AxiosResponse) => void) {
    axios
      .post("/api/access-request", data)
      .then(r => handleResponse(r))
      .catch(t => console.log('caught: ' + t))
  }

  handleAjaxException(endPoint: string, throwable: any) {
    console.log('caught: ' + throwable);
  }

}
