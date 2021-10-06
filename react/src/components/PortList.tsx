import React from "react";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Icon from "@mui/icons-material/FlightLand";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";


interface IProps {
  ports: string[];
  drtDomain: string;
}

export default class PortList extends React.Component<IProps> {
  render() {
    return <div>
      <h1>Welcome to DRT</h1>
      <p>Select your destination</p>
      <List>
        {this.props.ports.sort().map((portCode) => {
          let portCodeLC = portCode.toLowerCase();
          const url = 'https://' + portCodeLC + '.' + this.props.drtDomain;
          return <div key={portCode}>
            <ListItem button component="a" href={url} id={"port-link-" + portCodeLC}>
              <ListItemIcon><Icon/></ListItemIcon>
              <ListItemText primary={portCode.toUpperCase()}/>
            </ListItem>
            <Divider variant="inset" component="li"/>
          </div>
        })}
      </List>
    </div>
  }
}
