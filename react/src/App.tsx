import React, {useEffect} from 'react'
import './App.css'
import {Home} from './components/Home'
import Alerts from './components/alerts/Alerts'
import AccessRequests from './components/accessrequests/AccessRequests'
import UsersList from './components/users/UsersList'
import {Route, Routes, useNavigate, useParams} from "react-router-dom"
import Loading from "./components/Loading"
import {useConfig} from "./store/config"
import {Container} from "@mui/material"
import {styled} from "@mui/material/styles"
import {RegionPage} from "./components/RegionPage"
import axios from "axios"
import ApiClient from "./services/ApiClient"
import {HealthCheckEditor} from "./components/healthcheckpauseseditor/HealthCheckPausesEditor"
import {useUser} from "./store/user"
import {DropInSessionsList} from "./components/dropins/DropInSessionsList"
import {AddOrEditDropInSession} from "./components/dropins/AddOrEditDropInSession"
import {DropInSessionRegistrations} from "./components/dropins/DropInSessionRegistrations"
import {SnackbarProvider} from 'notistack'
import {FeatureGuideList} from "./components/featureguides/FeatureGuideList"
import {FeatureGuideAddOrEdit} from "./components/featureguides/FeatureGuideAddOrEdit"
import {FeedbackForms} from "./components/feedback/FeedbackForms"
import {FeedbackList} from "./components/feedback/FeedbackList"
import DownloadManager from './components/downloadmanager/DownloadManager'
import {ExportConfig} from "./components/ExportConfig"
import {HealthChecks} from "./components/healthchecks/HealthChecks"
import RegionalDashboard from './components/regionalpressure/RegionalDashboard'
import NationalDashboard from "./components/regionalpressure/NationalDashboard"
import {Header, BottomBar, AccessibilityStatement} from 'drt-react';
import {adminMenuItems} from './components/Navigation';
import {AirportNameIndex, getAirportByCode} from './airports';

const StyledContainer = styled(Container)(() => ({
  textAlign: 'left',
  minHeight: 500,
  display: 'inline-block',
  maxWidth: 'none !important',
  paddingTop: '16px',
  paddingBottom: '16px',
  flexGrow: 1,
}));

export const App = () => {
  const {user} = useUser()
  const {config} = useConfig()
  const navigate = useNavigate();
  const logoutLink = () => window.location.href = `/oauth2/sign_out?redirect=${window.location.origin}`;
  const isSignedIn = user.kind === "SignedInUser";
  const hasConfig = config.kind !== 'PendingConfig';

  const availablePorts = hasConfig ? config.values.ports.map(port => port.iata) : [];
  availablePorts.map((portCode) => getAirportByCode(portCode));

  const portMenuItems = [
    {
      label:'National Dashboard',
      link: '/national-pressure'
    },
    ...availablePorts.map(portCode => { 
      let domain = hasConfig && config.values.domain ? config.values.domain : 'drt-preprod'
      return {
        label: `${portCode} (${AirportNameIndex[portCode]})`,
        link: `https://${portCode.toLowerCase()}.${domain}`,
      }
    })
  ]

  useEffect(() => {
    const trackUser = async () =>
      axios
        .get(ApiClient.userTrackingEndPoint)
        .catch(reason => {
          console.log('Unable to user tracking' + reason)
        })

    trackUser()
  }, [])

  const routingFunction = (path: string) => {
    if (path.substring(0,4) == 'http') {
      window.location.href = path
    } else {
      navigate(path);
    }
  }

  const onClickAccessibilityStatement = () => {
    navigate('/accessibility/statement');
  }

  const getRandomAB = (): string => {
    return Math.random() < 0.5 ? 'A' : 'B';
  };

  const AccessibilityStatementWrapper = () => {
    const params = useParams()
    const scrollSection = params['scrollSection'] ? params['scrollSection'] : ''
    return (
      <AccessibilityStatement
        accessibilityStatementUrl="/accessibility"
        teamEmail="your-team-email@example.com"
        sendReportProblemGaEvent={() => console.log('Report Problem')}
        scrollSection={scrollSection || ''}
      />
    );
  };

  const roles = isSignedIn && user.profile?.roles;

  return (user.kind === "SignedInUser" && config.kind === "LoadedConfig") ?
    <div id="root">
      <div id="content">
        <Header
          routingFunction={routingFunction}
          logoutLink={logoutLink}
          userRoles={roles as string[]}
          adminMenuItems={adminMenuItems}
          rightMenuItems={[]}
          leftMenuItems={[]}
          maxWidth='none'
          initialSelectedPortMenuItem={''}
          portMenuItems={portMenuItems}/>
        <StyledContainer>
          <SnackbarProvider
            anchorOrigin={{
              vertical: 'top',
              horizontal: 'center'
            }}
          />
          <Routes>
            <Route path={"/"} element={<Home config={config.values} user={user.profile}/>}/>
            <Route path={"/accessibility/:scrollSection"} element={<AccessibilityStatementWrapper/>}/>
            <Route path={"/access-requests"} element={<AccessRequests/>}/>
            <Route path={"/users"} element={<UsersList/>}/>
            <Route path={"/download"} element={<DownloadManager config={config.values} user={user.profile}/>}/>
            <Route path={"/national-pressure"}
                   element={<NationalDashboard config={config.values} user={user.profile}/>}/>
            <Route path={"/national-pressure/:region"}
                   element={<RegionalDashboard config={config.values} user={user.profile}/>}/>
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
      </div>
      <footer id="footer" role="contentinfo">
        <BottomBar
          email="your-email@example.com"
          onClickAccessibilityStatement={() => onClickAccessibilityStatement()}
          accessibilityStatementUrl={'/accessibility/statement'}
          feedbackUrl={`${window.location.origin}/feedback/banner/${getRandomAB()}`}
        />
      </footer>
    </div> : <Loading/>
}
