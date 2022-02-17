import React from "react";
import {Box} from "@mui/material";
import {PortRegion} from "../model/Config";
import {PortListStraight} from "./PortListStraight";
import {PortListByRegion} from "./PortListByRegion";


interface IProps {
  allRegions: PortRegion[]
  userPorts: string[];
  drtDomain: string;
}

export const PortList = (props: IProps) => {
  const userRegions: PortRegion[] = props.allRegions.map(region => {
    const userPorts: string[] = props.userPorts.filter(p => region.ports.includes(p));
    return {...region, ports: userPorts} as PortRegion
  }).filter(r => r.ports.length > 0)

  return <Box sx={{width: '100%'}}>
    <h1>Welcome to DRT</h1>
    <p>Select your destination</p>
    {userRegions.length > 1 ?
      <PortListByRegion regions={userRegions} drtDomain={props.drtDomain}/> :
      <PortListStraight ports={props.userPorts} drtDomain={props.drtDomain}/>
    }
  </Box>
}

