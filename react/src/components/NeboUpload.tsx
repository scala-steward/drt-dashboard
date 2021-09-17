import React from 'react';
import axios, {AxiosRequestConfig, AxiosResponse} from "axios";
import {UserProfile} from "../model/User";
import {ConfigValues} from "../model/Config";
import {Button} from "@material-ui/core";
import ApiClient from "../services/ApiClient";

interface IProps {
  user: UserProfile;
  config: ConfigValues;
}

interface IState {
  selectedFile: any;
  fileInput: any;
  displayMessage: string[];
  hasError: boolean;
  errorMessage: string;
  showUploadButton: boolean;
}

interface FeedStatus {
  portCode: string;
  flightCount: string;
  statusCode: string;
}

class NeboUpload extends React.Component<IProps, IState> {

  constructor(props: IProps) {
    super(props);
    this.state = {
      selectedFile: null,
      fileInput: React.createRef(),
      displayMessage: [],
      hasError: false,
      errorMessage: '',
      showUploadButton: false
    };
  }

  onFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files.length > 0) {
      this.setState({selectedFile: event.target.files[0]});
      this.setState({showUploadButton: true});
      this.setState({displayMessage: []});
      this.setState({hasError: false});
    }
  };

  onFileUpload = () => {
    if (this.state.selectedFile) {
      const formData = new FormData();
      formData.append(
        "csv",
        this.state.selectedFile,
        this.state.selectedFile.name
      );
      this.postUploadData(ApiClient.neboFileUpload, formData, this.responseData, this.afterPostUploadData);
    }
  };

  afterPostUploadData = () => {
    const blankFileInput = {...this.state.fileInput, current: {...this.state.fileInput.current, value: ''}}
    this.setState({...this.state, selectedFile: null, showUploadButton: false, fileInput: blankFileInput})
  };

  public reqConfig: AxiosRequestConfig = {
    headers: {'Access-Control-Allow-Origin': '*'}
  };

  public postUploadData(endPoint: string, data: any, handleResponse: (r: AxiosResponse) => void, afterPost: () => void) {
    axios
      .post(endPoint, data, this.reqConfig)
      .then(response => handleResponse(response))
      .catch(t => this.setState(() => ({
        hasError: true,
        errorMessage: t,
        displayMessage: [...this.state.displayMessage, this.state.selectedFile.name + ' failed to upload. There was a problem processing your file, try again or contact us at ' + this.props.config.teamEmail + ' if it persists']
      })))
      .then(afterPost)
  }

  generateMessage(portCode: string, fileName: String, message: string) {
    return 'For port ' + portCode + ', ' + fileName + message;
  }

  responseData = (response: AxiosResponse) => {
    const feedStatusArray = response.data as FeedStatus[];
    feedStatusArray.map(feedStatus => {
      if (feedStatus.statusCode !== '202 Accepted') {
        this.setState({hasError: true});
        this.setState({
          displayMessage: [...this.state.displayMessage, this.generateMessage(feedStatus.portCode, this.state.selectedFile.name, ' failed to upload. Please contact us at ' + this.props.config.teamEmail)]
        });
      } else {
        if (feedStatus.flightCount === '0') {
          this.setState({hasError: true});
          this.setState({
            displayMessage: [...this.state.displayMessage, this.generateMessage(feedStatus.portCode, this.state.selectedFile.name, ' failed to upload. Check your file as no lines are parsed, try again later or contact us at ' + this.props.config.teamEmail)]
          });
        } else {
          this.setState({
            displayMessage: [...this.state.displayMessage, this.generateMessage(feedStatus.portCode, this.state.selectedFile.name, ' Arrivals have been updated. Thank you!')]
          });
        }
      }
      console.log('response feed ' + feedStatus.portCode + ' ' + feedStatus.flightCount + ' ' + feedStatus.statusCode);
      return null
    });
    console.log('response from post ' + response);
  }

  displayMessageWithCss(message: string) {
    if (message.includes("failed")) {
      return <div className="upload-error">{message}</div>
    } else {
      return <div className="upload-success">{message}</div>
    }
  }

  fileData = () => {
      if (this.state.selectedFile) {
      return (
        <div>
          <h2>File details:</h2>
          <p>File name: {this.state.selectedFile.name}</p>
          <p>File type: {this.state.selectedFile.type}</p>
          <p>
            Last modified:{" "}
            {this.state.selectedFile.lastModifiedDate.toDateString()}
          </p>

        </div>
      );
    } else {
      const messageToDisplay = this.state.displayMessage.map(m => this.displayMessageWithCss(m));
        return (
        <div>
          <br/>
          <h4>{messageToDisplay}</h4>
        </div>
      );
    }
  };

  render() {
    let page;
    if (this.props.user.roles.includes("nebo:upload")) {
      page = <div>
        <h1>
          Nebo data upload area
        </h1>
        <br/>
        <br/>
        <h3>Upload your CSV file</h3>
        <div>
          <br/>
          <input className="file-input" type="file" onChange={this.onFileChange} id="fileInputId"
                 ref={this.state.fileInput} accept=".csv"/>
          <br/>
          <br/>
          <br/>
        </div>
        {this.fileData()}
        {this.state.showUploadButton &&
        <Button variant="outlined" color="primary" onClick={this.onFileUpload}>Upload</Button>}
      </div>
    } else {
      page = <div>
        <h1>
          Nebo data upload area
        </h1>
        <br/>
        <br/>
        <span className="upload-error">You do not have permission to upload . Please contact us at drtpoiseteam@homeoffice.gov.uk if you need permission.</span>
        <br/>
        <br/>
        <br/>
      </div>
    }
    return (page);
  }
}

export default NeboUpload;
