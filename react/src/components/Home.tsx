import React from 'react';
import axios, {AxiosResponse} from 'axios';
import Button from '@material-ui/core/Button';
import Checkbox from '@material-ui/core/Checkbox';
import Divider from '@material-ui/core/Divider';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import Icon from "@material-ui/icons/FlightLand";
import {TextField, FormControl} from '@material-ui/core';

import ApiClient from '../services/ApiClient';

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

interface IAccessForm {
  portsRequested: string[];
  staffing: boolean;
  lineManager: string;
}

interface IState {
  user?: User;
  config: Config;
  accessForm: IAccessForm;
  requestFinished: boolean;
}

export default class Home extends React.Component<IProps, IState> {
  apiClient: ApiClient;

  constructor(props: IProps) {
    super(props);

    this.state = {
      config: {
        ports: [],
        domain: "",
      },
      accessForm: {
        portsRequested: [],
        staffing: false,
        lineManager: "",
      },
      requestFinished: false,
    }

    this.apiClient = new ApiClient()

    this.handleLineManagerChange = this.handleLineManagerChange.bind(this);
    this.handleAccessRequest = this.handleAccessRequest.bind(this);
  }

  componentDidMount() {
    this.apiClient.fetchData(this.apiClient.userEndPoint, this.updateUserState);
    this.apiClient.fetchData(this.apiClient.configEndPoint, this.updateConfigState);
  }

  updateUserState = (response: AxiosResponse) => {
    let user = response.data as UserLike;
    this.setState({...this.state, user: user});
  }

  updateConfigState = (response: AxiosResponse) => {
    let config = response.data as Config;
    this.setState({...this.state, config: config});
  }

  handlePortSelectionChange = (portCode: string, state: IState) => () => {
    let newPortsRequested: string[] = state.accessForm.portsRequested;

    if (state.accessForm.portsRequested.indexOf(portCode) === -1)
      newPortsRequested.push(portCode);
    else
      newPortsRequested = state.accessForm.portsRequested.filter(p => p !== portCode);

    const updatedAccessForm = {...state.accessForm, portsRequested: newPortsRequested}

    return {...state, accessForm: updatedAccessForm}
  };

  handleLineManagerChange = (newValue: string) => {
    const updatedAccessForm = {...this.state.accessForm, lineManager: newValue}

    return {...this.state, accessForm: updatedAccessForm};
  };

  toggleStaffing = () => {
    let updatedAccessForm;

    if (this.state.accessForm.staffing)
      updatedAccessForm = {...this.state.accessForm, staffing: false}
    else
      updatedAccessForm = {...this.state.accessForm, staffing: true}

    this.setState({...this.state, accessForm: updatedAccessForm});
  };

  setRequestFinished = (state: IState) => () => this.setState({...state, requestFinished: true})

  handleAccessRequest = (state: IState, handleResponse: (state: IState) => (r: AxiosResponse) => void) => {
    this.apiClient.sendData(this.apiClient.requestAccessEndPoint, state.accessForm, handleResponse(state));
  }

  render() {
    let stuff;

    if (this.state.user === undefined)
      stuff = <p>Loading..</p>
    else if (this.state.requestFinished)
      stuff = <div>Thanks for your request. We'll get back to you shortly</div>
    else if (this.state.user.ports.length === 0)
      stuff = this.RequestAccessForm(this.state.config.ports, this.state.accessForm)
    else
      stuff = this.PortsList(this.state.user.ports, this.state.config.domain);

    return (
      <header className="App-header">
        <h1>Welcome to DRT</h1>
        {stuff}
      </header>
    )
  }

  private RequestAccessForm(portsAvailable: string[], accessForm: IAccessForm) {
    return <div>
      <p>Please select the ports you require access to</p>
      <List>
        {portsAvailable.map((portCode) => {
          return <ListItem button onClick={() => this.setState(this.handlePortSelectionChange(portCode, this.state))}>
            <ListItemIcon>
              <Checkbox
                inputProps={{'aria-labelledby': portCode}}
                name={portCode}
                checked={accessForm.portsRequested.includes(portCode)}
              />
            </ListItemIcon>
            <ListItemText id={portCode} primary={portCode.toUpperCase()}/>
          </ListItem>
        })}
        <Divider/>
        <ListItem button onClick={this.toggleStaffing}>
          <ListItemIcon>
            <Checkbox
              inputProps={{'aria-labelledby': "staffing"}}
              name="staffing"
              checked={accessForm.staffing}
            />
          </ListItemIcon>
          <ListItemText id="staffing" primary="I require staffing access as my role includes planning"/>
        </ListItem>
        <ListItem>
          <FormControl fullWidth>
            <TextField
              id="outlined-helperText"
              label="Line manager's email address"
              helperText="Optional. May be helpful if we need to query your request"
              variant="outlined"
              onChange={event => this.setState(this.handleLineManagerChange(event.target.value))}
            />
          </FormControl>
        </ListItem>
      </List>

      <Button disabled={accessForm.portsRequested.length === 0}
              onClick={() => this.handleAccessRequest(this.state, this.setRequestFinished)}
              variant="contained"
              color="primary">
        Request access
      </Button>
    </div>;
  }

  private PortsList(ports: string[], drtDomain: string) {
    return <div>
      <p>Select your destination</p>
      <List>
        {ports.map((portCode) => {
          const url = 'https://' + portCode + '.' + drtDomain;
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
