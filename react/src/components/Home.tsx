import React from 'react';
import axios from 'axios';
import Button from '@material-ui/core/Button';
import Checkbox from '@material-ui/core/Checkbox';
import Link from '@material-ui/core/Link';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import Icon from "@material-ui/icons/FlightLand";

interface UserLike {
  email: string;
  ports: string[];
}

class User implements UserLike {
  email: string;
  ports: string[];

  constructor(email: string, ports: string[]) {
    this.email = email;
    this.ports = ports;
  }
}

interface Config {
  ports: string[];
  domain: string;
}

interface IProps {
}

interface IState {
  config?: Config;
  user?: User;
  portsRequested: string[];
  requestFinished: boolean;
}

export default class Home extends React.Component<IProps, IState> {
  constructor(props: IProps) {
    super(props);

    this.state = {
      portsRequested: [],
      requestFinished: false
    }

    this.handlePortSelectionChange = this.handlePortSelectionChange.bind(this)
    this.handlePortSelectionChange2 = this.handlePortSelectionChange2.bind(this)
    this.handleAccessRequest = this.handleAccessRequest.bind(this)
  }

  componentDidMount() {
    axios
      .get("/api/user")
      .then(res => {
        let user = res.data as UserLike;
        this.setState({...this.state, user: user})
      })
      .catch(t => console.log('caught: ' + t))

    axios
      .get("/api/config")
      .then(res => {
        let config = res.data as Config;
        console.log("config ports: " + config.ports)
        this.setState({...this.state, config: config})
      })
      .catch(t => console.log('caught: ' + t))
  }

  handlePortSelectionChange(event: React.ChangeEvent<HTMLInputElement>) {
    const target = event.target;
    const checked = target.checked;
    const port = target.name;

    let newPortsRequested: string[] = this.state.portsRequested

    if (checked) newPortsRequested.push(port)
    else newPortsRequested = this.state.portsRequested.filter(p => p !== port)
    console.log("portsRequested: " + this.state.portsRequested)

    this.setState({...this.state, portsRequested: newPortsRequested});
  }

  handlePortSelectionChange2 = (portCode: string) => () => {
    let newPortsRequested: string[] = this.state.portsRequested;

    if (this.state.portsRequested.indexOf(portCode) === -1)
      newPortsRequested.push(portCode);
    else
      newPortsRequested = this.state.portsRequested.filter(p => p !== portCode);

    this.setState({...this.state, portsRequested: newPortsRequested});
  };

  handleAccessRequest(_: React.MouseEvent<HTMLButtonElement>) {
    axios
      .post("/api/request-access", {ports: this.state.portsRequested})
      .then(_ => {
        this.setState({...this.state, requestFinished: true})
      })
      .catch(t => console.log('caught: ' + t))
  }

  render() {
    let stuff;

    if (this.state.user === undefined)
      stuff = <p>Loading..</p>
    else if (this.state.requestFinished)
      stuff = <div>Thanks for your request. We'll get back to you shortly</div>
    else if (this.state.user.ports.length === 0)
      stuff = <div>
        <p>Please select the ports you require access to</p>
        <List>
          {this.state.config?.ports.map((portCode) => {
            return <ListItem button onClick={this.handlePortSelectionChange2(portCode)}>
              <ListItemIcon>
                <Checkbox
                  inputProps={{ 'aria-labelledby': portCode }}
                  name={portCode}
                  checked={this.state.portsRequested.includes(portCode)}
                />
              </ListItemIcon>
              <ListItemText id={portCode} primary={portCode.toUpperCase()}/>
            </ListItem>
          })}
        </List>

        <Button disabled={this.state.portsRequested.length === 0}
                onClick={this.handleAccessRequest}
                variant="contained"
                color="primary">
          Request access
        </Button>
      </div>
    else
      stuff = <div><p>Select your destination</p>
        <List>
          {this.state.user.ports.map((portCode) => {
            const domain = this.state.config?.domain;
            const url = 'https://' + portCode + '.' + domain;
            return <ListItem button component="a" href={url}>
              <ListItemIcon><Icon/></ListItemIcon>
              <ListItemText primary={portCode}/>
            </ListItem>
          })}
        </List>
      </div>

    return (
      <header className="App-header">
        <h1>Welcome to DRT</h1>
        {stuff}
      </header>
    )
  }
}
