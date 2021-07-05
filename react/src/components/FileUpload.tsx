import React, {Component} from 'react';
import UserLike from "../model/User";
import ApiClient from "../services/ApiClient";
import axios, {AxiosRequestConfig, AxiosResponse} from "axios";

interface IProps {
    user: UserLike;
}

interface IState {
    selectedFile: any;
    fileInput: any;
    displayMessage: string;
    hasError: boolean;
    errorMessage: string;
    showUploadButton: boolean;

}

class FileUpload extends React.Component<IProps, IState> {

    constructor(props: IProps) {
        super(props);
        this.state = {
            selectedFile: null,
            fileInput: React.createRef(),
            displayMessage: '',
            hasError: false,
            errorMessage: '',
            showUploadButton: false
        };
    }

    onFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        if (event.target.files && event.target.files.length > 0) {
            this.setState({selectedFile: event.target.files[0]});
            this.setState({showUploadButton: true});
            this.setState({displayMessage: ''});
            this.setState({hasError: false});
        }
    };

    onFileUpload = () => {
        const apiClient = new ApiClient;
        if (this.state.selectedFile) {
            const formData = new FormData();
            formData.append(
                "csv",
                this.state.selectedFile,
                this.state.selectedFile.name
            );
            this.postUploadData("/uploadFile", formData, this.responseData);
            if (this.state.hasError) {
                this.setState({displayMessage: this.state.selectedFile.name + ' failed to upload. Check your file, try again later or contact us at drtpoiseteam@homeoffice.gov.uk.'});
            } else {
                this.setState({displayMessage: 'Thank you! Arrivals have been updated.'});
            }

            this.setState({selectedFile: null});
            this.setState({showUploadButton: false});
            this.state.fileInput.current.value = '';
        }

    };

    public reqConfig: AxiosRequestConfig = {
        headers: {'Access-Control-Allow-Origin': '*'}
    };

    public postUploadData(endPoint: string, data: any, handleResponse: (r: AxiosResponse) => void) {
        let fileName = this.state.selectedFile.name;
        axios
            .post(endPoint, data, this.reqConfig)
            .then(response => handleResponse(response))
            .catch(t => this.setState(() => ({
                hasError: true,
                errorMessage: t,
                displayMessage: fileName + ' failed to upload. Check your file, try again later or contact us at drtpoiseteam@homeoffice.gov.uk.'
            })))
    }

    responseData = (response: AxiosResponse) => {
        console.log('response from post ' + response)
    }


    fileData = () => {
        let message;
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
            if (this.state.hasError) {
                message = <h4 className="upload-error">{this.state.displayMessage}</h4>
            } else {
                message = <h4 className="upload-success">{this.state.displayMessage}</h4>
            }
            return (
                <div>
                    <br/>
                    {message}
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
                       <input className="file-input" type="file" onChange={this.onFileChange} id="fileInputId" ref={this.state.fileInput} accept=".csv"/>
                    <br/>
                    <br/>
                    <br/>
                       {this.state.showUploadButton && <button className="upload-button" onClick={this.onFileUpload}>Upload</button>}
                </div>
                {this.fileData()}
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

export default FileUpload;