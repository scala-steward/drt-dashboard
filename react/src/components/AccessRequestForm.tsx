import React from "react";
import ApiClient from "../services/ApiClient";
import {AxiosResponse} from "axios";
import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import Checkbox from "@material-ui/core/Checkbox";
import ListItemText from "@material-ui/core/ListItemText";
import Divider from "@material-ui/core/Divider";
import {FormControl, TextField} from "@material-ui/core";
import Button from "@material-ui/core/Button";


interface IProps {
  ports: string[];
}

interface IState {
  portsRequested: string[];
  staffing: boolean;
  lineManager: string;
  requestSubmitted: boolean;
}

export default class AccessRequestForm extends React.Component<IProps, IState> {
  apiClient: ApiClient;

  constructor(props: IProps) {
    super(props);

    this.state = {
      portsRequested: [],
      staffing: false,
      lineManager: "",
      requestSubmitted: false,
    }

    this.apiClient = new ApiClient();

    this.handleAccessRequest = this.handleAccessRequest.bind(this);
  }

  handlePortSelectionChange = (portCode: string, state: IState) => () => {
    let newPortsRequested: string[] = state.portsRequested;

    if (state.portsRequested.indexOf(portCode) === -1)
      newPortsRequested.push(portCode);
    else
      newPortsRequested = state.portsRequested.filter(p => p !== portCode);

    return {...state, portsRequested: newPortsRequested};
  };

  handleLineManagerChange = (state: IState, newValue: string) => {
    return {...state, lineManager: newValue};
  };

  toggleStaffing = (state: IState) => {
    return {...state, staffing: !state.staffing};
  };

  setRequestFinished = (state: IState) => () => this.setState({...state, requestSubmitted: true});

  handleAccessRequest = (state: IState, handleResponse: (state: IState) => (r: AxiosResponse) => void) => {
    this.apiClient.sendData(this.apiClient.requestAccessEndPoint, state, handleResponse(state));
  }

  render() {
    let content;

    if (this.state.requestSubmitted)
      content = <div>Thanks for your request. We'll get back to you shortly</div>;
    else
      content = this.form(this.props.ports, this.state);

    return content;
  }

  private form(portsAvailable: string[], accessForm: IState) {
    return <div>
      <p>Please select the ports you require access to</p>
      <List>
        {portsAvailable.map((portCode) => {
          return <ListItem
            button
            key={portCode}
            onClick={() => this.setState(this.handlePortSelectionChange(portCode, this.state))}>
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
        <ListItem
          button
          key={'staffing'}
          onClick={() => this.setState(this.toggleStaffing(this.state))}>
          <ListItemIcon>
            <Checkbox
              inputProps={{'aria-labelledby': "staffing"}}
              name="staffing"
              checked={accessForm.staffing}
            />
          </ListItemIcon>
          <ListItemText id="staffing" primary="I require staffing access as my role includes planning"/>
        </ListItem>
        <ListItem key={'line-manager'}>
          <FormControl fullWidth>
            <TextField
              id="outlined-helperText"
              label="Line manager's email address"
              helperText="Optional. May be helpful if we need to query your request"
              variant="outlined"
              onChange={event => this.setState(this.handleLineManagerChange(this.state, event.target.value))}
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
}
