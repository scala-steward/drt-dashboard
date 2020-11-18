import React from "react";
import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import Icon from "@material-ui/icons/FlightLand";
import ListItemText from "@material-ui/core/ListItemText";
import Divider from "@material-ui/core/Divider";


interface IProps {
  ports: string[];
  drtDomain: string;
}

export default class PortList extends React.Component<IProps> {
  render() {
    return <div>
      <p>Select your destination</p>
      <List>
        {this.props.ports.map((portCode) => {
          let portCodeLC = portCode.toLowerCase();
          const url = 'https://' + portCodeLC + '.' + this.props.drtDomain;
          return <div key={portCode}>
            <ListItem button component="a" href={url} id={"port-link-" + portCodeLC}>
              <ListItemIcon><Icon/></ListItemIcon>
              <ListItemText primary={portCode}/>
            </ListItem>
            <Divider variant="inset" component="li" />
          </div>
        })}
      </List>
    </div>
  }
}
