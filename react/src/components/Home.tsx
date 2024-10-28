import * as React from 'react';
import AccessRequestForm from "./AccessRequestForm";
import {PortList} from "./PortList";
import { useNavigate } from 'react-router';
import {Box} from "@mui/material";
import {UserProfile} from "../model/User";
import {ConfigValues} from "../model/Config";
import {Helmet} from "react-helmet";
import PageContentWrapper from './PageContentWrapper';


interface IProps {
  user: UserProfile
  config: ConfigValues
}


export const Home = (props: IProps) => {
  const navigate = useNavigate();
  const isRccUser = () => {
    return props.user.roles.includes("rcc:central") || props.user.roles.includes("rcc:heathrow") || props.user.roles.includes("rcc:north") || props.user.roles.includes("rcc:south")
  }

  React.useEffect(() => {
    if (isRccUser() || props.user.roles.includes('national:view') || props.user.roles.includes('forceast:view')){
        navigate('/national-pressure');
    }
  });

  return <PageContentWrapper>
    <Helmet>
      <title>DRT</title>
    </Helmet>
    <Box className="App-header">
      {props.user.ports.length === 0 && !isRccUser() ?
        <AccessRequestForm regions={props.config.portsByRegion} teamEmail={props.config.teamEmail}/> :
        <PortList user={props.user} allRegions={props.config.portsByRegion} userPorts={props.user.ports}
                  drtDomain={props.config.domain} isRccUser={isRccUser()}/>
      }
    </Box>
  </PageContentWrapper>
}
