import React, {useEffect} from 'react';
import './App.css';
import useMediaQuery from '@mui/material/useMediaQuery';
import { useTheme } from '@mui/material/styles';
import {Home} from './components/Home';
import Alerts from './components/alerts/Alerts';
import AccessRequests from './components/accessrequests/AccessRequests';
import UsersList from './components/users/UsersList';
import {Route, Routes} from "react-router-dom";
import Loading from "./components/Loading";
import Navigation from "./components/Navigation";
import {useConfig} from "./store/config";
import {Container} from "@mui/material";
import {styled} from "@mui/material/styles";
import {RegionPage} from "./components/RegionPage";
import axios from "axios";
import ApiClient from "./services/ApiClient";
import {HealthCheckEditor} from "./components/healthcheckpauseseditor/HealthCheckPausesEditor";
import {useUser} from "./store/user";
import {DropInSessionsList} from "./components/dropins/DropInSessionsList";
import {AddOrEditDropInSession} from "./components/dropins/AddOrEditDropInSession";
import {DropInSessionRegistrations} from "./components/dropins/DropInSessionRegistrations";
import {SnackbarProvider} from 'notistack';
import Link from "@mui/material/Link";
import {FeatureGuideList} from "./components/featureguides/FeatureGuideList";
import {FeatureGuideAddOrEdit} from "./components/featureguides/FeatureGuideAddOrEdit";
import {FeedbackForms} from "./components/feedback/FeedbackForms";
import {FeedbackList} from "./components/feedback/FeedbackList";
import DownloadManager from './components/downloadmanager/DownloadManager';
import {ExportConfig} from "./components/ExportConfig";
import {HealthChecks} from "./components/healthchecks/HealthChecks";
import RegionalDashboard from './components/regionalpressure/RegionalDashboard';
import RegionalDashboardDetail from './components/regionalpressure/RegionalDashboardDetail';

const StyledDiv = styled('div')(() => ({
  textAlign: 'center',
}));

const StyledContainer = styled(Container)(() => ({
  textAlign: 'left',
  minHeight: 500,
  display: 'inline-block',
}));

export const App = () => {
  const {user} = useUser()
  const {config} = useConfig()

  const currentLocation = window.document.location;
  const logoutLink = "/oauth2/sign_out?redirect=" + currentLocation.toString()
  const theme = useTheme();
  const is_mobile = useMediaQuery(theme.breakpoints.down('md'));

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
              <Link href="https://www.gov.uk" title="Go to the GOV.UK homepage"
                    id="global-header-logo"
                    className="content"
                    sx={{display: 'flex', gap: 1, alignItems: 'center', justifyItems: 'center'}}>
                <img src="/images/gov.uk-white.svg" width="150" height="35" alt="GOV.UK"/>
              </Link>
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
      <StyledContainer disableGutters={is_mobile}>
        <SnackbarProvider
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'center'
          }}
        />
        <Routes>
          <Route path={"/"} element={<Home config={config.values} user={user.profile}/>}/>
          <Route path={"/access-requests"} element={<AccessRequests/>}/>
          <Route path={"/users"} element={<UsersList/>}/>
          <Route path={"/download"} element={<DownloadManager config={config.values} user={user.profile} />} />
          <Route path={"/national-pressure"} element={<RegionalDashboard config={config.values} user={user.profile} />} />
          <Route path={"/national-pressure/:region"} element={<RegionalDashboardDetail config={config.values} user={user.profile} />} />
          <Route path={"/alerts"} element={<Alerts regions={config.values.portsByRegion} user={user.profile}/>}/>
          <Route path={"/region/:regionName"} element={<RegionPage user={user.profile} config={config.values}/>}/>
          <Route path={"/feature-guides"}>
            <Route path={""} element={<FeatureGuideList/>}/>
            <Route path={"edit"} element={<FeatureGuideAddOrEdit/>}/>
            <Route path={"edit/:guideId"} element={<FeatureGuideAddOrEdit/>}/>
          </Route>
          <Route path={"/drop-in-sessions"}>
            <Route path={""} element={<DropInSessionsList/>}/>
            <Route path={"list"} element={<DropInSessionsList/>}/>
            <Route path={"edit"} element={<AddOrEditDropInSession/>}/>
            <Route path={"edit/:dropInId"} element={<AddOrEditDropInSession/>}/>
            <Route path={"list/registered-users/:dropInId"} element={<DropInSessionRegistrations/>}/>
          </Route>
          <Route path={"/health-checks"} element={<HealthChecks portsByRegion={config.values.portsByRegion}/>}/>
          <Route path={"/health-check-pauses"} element={<HealthCheckEditor/>}/>
          <Route path={"/feedback/:feedbackType/:abVersion"} element={<FeedbackForms/>}/>
          <Route path={"/user-feedback"} element={<FeedbackList/>}/>
          <Route path={"/export-config"} element={<ExportConfig/>}/>
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
