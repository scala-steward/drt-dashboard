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

export default class PortsList extends React.Component<IProps> {
  render() {
    return <div>
      <p>Select your destination</p>
      <List>
        {this.props.ports.map((portCode) => {
          const url = 'https://' + portCode + '.' + this.props.drtDomain;
          return <div>
            <ListItem button component="a" href={url}>
              <ListItemIcon><Icon/></ListItemIcon>
              <ListItemText primary={portCode}/>
            </ListItem>
            <Divider variant="inset" component="li"/>
          </div>
        })}
      </List>
    </div>
  }
}
