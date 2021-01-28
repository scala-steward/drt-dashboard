import axios, {AxiosResponse} from "axios";


interface IApiClient {
  fetchData: (userEndPoint: string, handleResponse: (r: AxiosResponse) => void) => void
}

export default class ApiClient implements IApiClient {
  public userEndPoint = "/api/user";
  public configEndPoint = "/api/config";
  public requestAccessEndPoint = "/api/request-access";
  public logoutEndPoint = "/oauth/logout";
  public alertsEndPoint = "/api/alerts";

  public fetchData(userEndPoint: string, handleResponse: (r: AxiosResponse) => void) {
    axios
      .get(userEndPoint)
      .then(response => handleResponse(response))
      .catch(t => this.handleAjaxException(userEndPoint, t))
  }

  public sendData(userEndPoint: string, data: any, handleResponse: (r: AxiosResponse) => void) {
    axios
      .post("/api/request-access", data)
      .then(r => handleResponse(r))
      .catch(t => console.log('caught: ' + t))
  }

  handleAjaxException(endPoint: string, throwable: any) {
    console.log('caught: ' + throwable);
    // window.document.location.reload();
  }
}
