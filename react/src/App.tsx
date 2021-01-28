import React from 'react';
import './App.css';
import Home from './components/Home';
import Alerts from './components/Alerts/Alerts';
import {BrowserRouter as Router, Route} from "react-router-dom";
import ApiClient from "./services/ApiClient";
import {AxiosResponse} from "axios";
import Loading from "./components/Loading";
import Navigation from "./components/Navigation";

interface UserLike {
    email: string;
    ports: string[];
    roles: string[];
}

interface Config {
    ports: string[];
    domain: string;
}

interface IProps {
}

interface IState {
    user?: UserLike;
    config?: Config;
}

export default class App extends React.Component<IProps, IState> {
    apiClient: ApiClient;

    constructor(props: IProps) {
        super(props);

        this.apiClient = new ApiClient();
        this.state = {};
    }

    componentDidMount() {
        this.apiClient.fetchData(this.apiClient.userEndPoint, this.updateUserState);
        this.apiClient.fetchData(this.apiClient.configEndPoint, this.updateConfigState);
    }

    updateUserState = (response: AxiosResponse) => {
        let user = response.data as UserLike;
        this.setState({...this.state, user: user});
    }

    updateConfigState = (response: AxiosResponse) => {
        let config = response.data as Config;
        this.setState({...this.state, config: config});
    }

    render() {
        const currentLocation = window.document.location;
        const logoutLink = "/oauth/logout?redirect=" + currentLocation

        return (
            <div className="App">
                <Router>
                    <header role="banner" id="global-header" className=" with-proposition">
                        <div className="header-wrapper">
                            <div className="header-global">
                                <div className="header-logo">
                                    <a href="https://www.gov.uk" title="Go to the GOV.UK homepage"
                                       id="global-header-logo"
                                       className="content">
                                        <img
                                            src="images/gov.uk_logotype_crown_invert_trans.png"
                                            width="36" height="32" alt=""/> GOV.UK
                                    </a>
                                </div>
                            </div>
                            <div className="header-proposition">
                                <div className="logout">
                                    {this.state.user !== undefined ?
                                        <Navigation logoutLink={logoutLink} user={this.state.user !!}/>
                                        : ""
                                    }
                                </div>
                                <div className="content">
                                    <a href="/" id="proposition-name">Dynamic Response Tool</a>
                                </div>
                            </div>
                        </div>
                    </header>

                    <div id="global-header-bar"/>
                    <Route exact path="/">
                        {this.state.config === undefined || this.state.user === undefined ?
                            <Loading/> :
                            <Home config={this.state.config !!} user={this.state.user !!}/>
                        }
                    </Route>
                    <Route exact path="/alerts">
                        {this.state.user === undefined ?
                            <Loading/> :
                            <Alerts user={this.state.user}/>
                        }
                    </Route>
                </Router>
                <footer className="group js-footer" id="footer" role="contentinfo">
                    <div className="footer-wrapper">
                        <div className="footer-meta">
                            <div className="footer-meta-inner">
                            </div>
                        </div>
                    </div>
                </footer>
            </div>
        );
    }
}


