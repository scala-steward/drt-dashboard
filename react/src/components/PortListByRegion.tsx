import {styled} from "@mui/material/styles";
import {PortRegion} from "../model/Config";
import {Box} from "@mui/material";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Icon from "@mui/icons-material/FlightLand";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";
import React from "react";

const Region = styled('p')(() => ({
  fontSize: '21px',
  fontWeight: 'bold',
}));

export const PortListByRegion = (props: { regions: PortRegion[], drtDomain: string }) => {
  return <Box sx={{
      display: 'flex',
      flexWrap: 'wrap',
      justifyContent: 'space-start',
    }}
  >
    {props.regions.map((region) => {
      const sortedPorts = region.ports.sort()
      return <Box sx={{
        minWidth: '200px',
        marginRight: '50px'
      }}>
        <Region>{region.name}</Region>
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
