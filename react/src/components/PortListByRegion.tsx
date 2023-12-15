import {PortRegion} from "../model/Config";
import {Box} from "@mui/material";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Icon from "@mui/icons-material/FlightLand";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";
import React from "react";
import Button from '@mui/material/Button';
import {UserProfile} from "../model/User";

export const PortListByRegion = (props: { user: UserProfile, regions: PortRegion[], drtDomain: string }) => {
  return <Box sx={{
    display: 'flex',
    flexWrap: 'wrap',
    justifyContent: 'space-start',
  }}
  >
    {props.regions.map((region) => {
      const sortedPorts = region.ports.sort()
      return <Box
        sx={{minWidth: '200px', marginRight: '50px'}}
        key={region.name}
      >
        {props.user.roles.includes("rcc:" + region.name.toLowerCase()) ?
          <Button style={{fontSize: '18px',fontWeight:'bold'}}
                  href={`/region/${region.name.toLowerCase()}`}>{region.name}</Button> :
          <Button style={{fontSize: '18px', color: 'black',fontWeight:'bold'}} disabled={true}>{region.name}</Button>
        }
        <List>
          {sortedPorts.map((portCode) => {
            let portCodeLC = portCode.toLowerCase();
            const url = 'https://' + portCodeLC + '.' + props.drtDomain;
            return <Box key={portCode}>
              <ListItem button component="a" href={url} id={"port-link-" + portCodeLC}>
                <ListItemIcon><Icon/></ListItemIcon>
                <ListItemText primary={portCode.toUpperCase()}/>
              </ListItem>
              <Divider variant="inset" component="li"/>
            </Box>
          })}
        </List>
      </Box>
    })}
  </Box>
}
