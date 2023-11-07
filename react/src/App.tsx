import React, {useEffect} from 'react';
import './App.css';
import {Home} from './components/Home';
import Alerts from './components/alerts/Alerts';
import AccessRequests from './components/accessrequests/AccessRequests';
import UsersList from './components/users/UsersList';
import {Route, Routes} from "react-router-dom";
import Loading from "./components/Loading";
import Navigation from "./components/Navigation";
import {useConfig} from "./store/configSlice";
import {Container} from "@mui/material";
import {styled} from "@mui/material/styles";
import {RegionPage} from "./components/RegionPage";
import axios from "axios";
import ApiClient from "./services/ApiClient";
import UploadForm from "./components/featureguides/FeatureGuideUploadFile";
import {HealthCheckEditor} from "./components/HealthCheckPausesEditor";
import {useUser} from "./store/userSlice";
import {DropInsList} from "./components/dropins/DropInsList";
import {CreateDropIn} from "./components/dropins/CreateDropIn";
import {EditDropIn} from "./components/dropins/EditDropIn";
import {RegisteredUsers} from "./components/dropins/RegisteredUsers";

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

export const App = () => {
  const {user} = useUser()
  const {config} = useConfig()

  const currentLocation = window.document.location;
  const logoutLink = "/oauth/logout?redirect=" + currentLocation.toString()

  useEffect(() => {
    const trackUser = async () =>
      axios
        .get(ApiClient.userTrackingEndPoint)
        .catch(reason => {
          console.log('Unable to user tracking' + reason);
        })

    trackUser();
  }, []);

  return (user.kind === "SignedInUser" && config.kind === "LoadedConfig") ?
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
              {user.kind === "SignedInUser" &&
                  <Navigation logoutLink={logoutLink} user={user.profile}/>}
            </div>
            <div className="content">
              <a href="/" id="proposition-name">Dynamic Response Tool</a>
            </div>
          </div>
        </div>
      </header>

      <div id="global-header-bar"/>
      <StyledContainer>
        <Routes>
          <Route path="/" element={<Home config={config.values} user={user.profile}/>}/>
          <Route path="/access-requests" element={<AccessRequests/>}/>
          <Route path="/users" element={<UsersList/>}/>
          <Route path="/alerts" element={<Alerts regions={config.values.portsByRegion} user={user.profile}/>}/>
          <Route path="/region/:regionName" element={<RegionPage user={user.profile} config={config.values}/>}/>
          <Route path="/feature-guide-upload" element={<UploadForm/>}/>
          <Route path="/drop-ins">
            <Route path="" element={<DropInsList/>}/>
            <Route path="list/crud/:operations" element={<DropInsList/>}/>
            <Route path="list/:listAll?" element={<DropInsList/>}/>
            <Route path="new" element={<CreateDropIn/>}/>
            <Route path="edit/:dropInId" element={<EditDropIn/>}/>
            <Route path="list/registeredUsers/:dropInId" element={<RegisteredUsers/>}/>
          </Route>
          <Route path="/health-checks" element={<HealthCheckEditor/>}/>
        </Routes>
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
