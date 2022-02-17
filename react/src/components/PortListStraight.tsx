import {Box} from "@mui/material";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Icon from "@mui/icons-material/FlightLand";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";
import React from "react";

export const PortListStraight = (props: { ports: string[], drtDomain: string }) => {
  const sortedPorts = [...props.ports].sort()

  return <Box>
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
}
