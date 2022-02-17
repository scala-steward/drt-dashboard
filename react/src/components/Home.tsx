import React from 'react';

import AccessRequestForm from "./AccessRequestForm";
import {PortList} from "./PortList";
import {Box} from "@mui/material";
import {UserProfile} from "../model/User";
import {ConfigValues} from "../model/Config";


interface IProps {
  user: UserProfile;
  config: ConfigValues;
}

export const Home = (props: IProps) => {
  return <Box className="App-header">
    {props.user.ports.length === 0 ?
      <AccessRequestForm regions={props.config.regions} ports={props.config.ports} teamEmail={props.config.teamEmail}/> :
      <PortList allRegions={props.config.regions} userPorts={props.user.ports} drtDomain={props.config.domain}/>
    }
  </Box>
}

