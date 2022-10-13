import React from 'react';
import './App.css';
import {Home} from './components/Home';
import Alerts from './components/Alerts/Alerts';
import UserAccess from './components/UserManagement/UserAccess';
import {Route, Switch} from "react-router-dom";
import Loading from "./components/Loading";
import Navigation from "./components/Navigation";
import NeboUpload from "./components/NeboUpload";
import {RootState, rootStore} from "./store/rootReducer";
import {connect, ConnectedProps} from "react-redux";
import {fetchUserProfile} from "./store/userSlice";
import {fetchConfig} from "./store/configSlice";
import {RedListEditor} from "./components/RedListEditor";
import {Container} from "@mui/material";
import {styled} from "@mui/material/styles";
import {RegionalPort} from "./components/RegionalPort";


const StyledDiv = styled('div')(() => ({
    textAlign: 'center',
}));

const StyledContainer = styled(Container)(() => ({
    margin: 5,
    padding: 15,
    textAlign: 'left',
    minHeight: 500,
    display: 'inline-block',
}));

const regionName = new URLSearchParams(window.location.search).get('regionName')

rootStore.dispatch(fetchUserProfile())
rootStore.dispatch(fetchConfig())

const mapState = (state: RootState) => ({
    user: state.user,
    config: state.config
})

const connector = connect(mapState)

type PropsFromReact = ConnectedProps<typeof connector>

const App = (props: PropsFromReact) => {
    const currentLocation = window.document.location;
    const logoutLink = "/oauth/logout?redirect=" + currentLocation.toString()

    return (props.user.kind === "SignedInUser" && props.config.kind === "LoadedConfig") ?
        <StyledDiv>
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
                            {props.user.kind === "SignedInUser" &&
                            <Navigation logoutLink={logoutLink} user={props.user.profile}/>}
                        </div>
                        <div className="content">
                            <a href="/" id="proposition-name">Dynamic Response Tool</a>
                        </div>
                    </div>
                </div>
            </header>

            <div id="global-header-bar"/>
            <StyledContainer>
                <Switch>
                    <Route exact path="/">
                        <Home config={props.config.values} user={props.user.profile}/>
                    </Route>
                    <Route exact path="/userManagement">
                        <UserAccess/>
                    </Route>
                    <Route exact path="/alerts">
                        <Alerts regions={props.config.values.portsByRegion} user={props.user.profile}/>
                    </Route>
                    <Route exact path="/upload">
                        <NeboUpload user={props.user.profile} config={props.config.values}/>
                    </Route>
                    <Route exact path="/region">
                        <RegionalPort user={props.user.profile} config={props.config.values} region={regionName}/>
                    </Route>
                    <Route exact path="/red-list-editor">
                        <RedListEditor/>
                    </Route>
                </Switch>
            </StyledContainer>
            <footer className="group js-footer" id="footer" role="contentinfo">
                <div className="footer-wrapper">
                    <div className="footer-meta">
                        <div className="footer-meta-inner">
                        </div>
                    </div>
                </div>
            </footer>
        </StyledDiv> : <Loading/>
}

export default connector(App)
