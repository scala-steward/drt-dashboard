import React from "react";
import {styled} from '@mui/material/styles';
import ApiClient from "../services/ApiClient";
import axios from "axios";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Checkbox from "@mui/material/Checkbox";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";
import {Box, FormControl, Paper, TextField, Typography} from "@mui/material";
import Button from "@mui/material/Button";


const StyledPaper = styled(Paper)(({theme}) => ({
  textAlign: "left",
  padding: theme.spacing(2),
  width: '100%',
}))

const StyledTypography = styled(Typography)(() => ({
  fontWeight: "bold"
}))

const DeclarationUl = styled('ul')(({theme}) => ({
  ...theme.typography.body1,
  listStyleType: "circle"
}));

const ThankYouBox = styled(Box)(() => ({
  width: "75%"
}));

interface IProps {
  ports: string[];
  teamEmail: string;
}

interface IState {
  allPorts: boolean;
  portsRequested: string[];
  staffing: boolean;
  lineManager: string;
  agreeDeclaration: boolean;
  requestSubmitted: boolean;
}

export default function AccessRequestForm(props: IProps) {
  const [state, setState] = React.useState(
    {
      allPorts: false,
      portsRequested: [],
      staffing: false,
      lineManager: "",
      agreeDeclaration: false,
      requestSubmitted: false,
    } as IState);


  const updatePortSelection = (port: string) => {

    const requested: string[] = state.portsRequested.includes(port) ?
      state.portsRequested.filter(value => value !== port) :
      [
        ...state.portsRequested,
        port
      ]

    setState({
      ...state,
      portsRequested: requested
    });
  };

  const handleLineManagerChange = (state: IState, newValue: string) => {
    return {...state, lineManager: newValue};
  };

  const setRequestFinished = () => setState({...state, requestSubmitted: true});

  const save = () => axios.post(ApiClient.requestAccessEndPoint, state)
    .then(setRequestFinished)
    .then(() => axios.get(ApiClient.logoutEndPoint))
    .then(() => console.log("User has been logged out."))


  function form(portsAvailable: string[]) {
    const sortedPorts = [...portsAvailable].sort()
    return <div>
      <h1>Welcome to DRT</h1>
      <p>Please select the ports you require access to</p>
      <List>
        <ListItem
          button
          key={'allStaff'}
          onClick={() => setState({...state, allPorts: !state.allPorts})}>
          <ListItemIcon>
            <Checkbox
              inputProps={{'aria-labelledby': "allPorts"}}
              name="allPorts"
              checked={state.allPorts}
            />
          </ListItemIcon>
          <ListItemText id="allPorts" primary="All ports"/>
        </ListItem>
        {state.allPorts ? null : sortedPorts.map((portCode) => {
          return <ListItem
            button
            key={portCode}
            onClick={() => updatePortSelection(portCode)}
          >
            <ListItemIcon>
              <Checkbox
                inputProps={{'aria-labelledby': portCode}}
                name={portCode}
                checked={state.portsRequested.includes(portCode)}
              />
            </ListItemIcon>
            <ListItemText id={portCode} primary={portCode.toUpperCase()}/>
          </ListItem>
        })}
        <Divider/>
        <ListItem
          button
          key={'staffing'}
          onClick={() => setState({...state, staffing: !state.staffing})}>
          <ListItemIcon>
            <Checkbox
              inputProps={{'aria-labelledby': "staffing"}}
              name="staffing"
              checked={state.staffing}
            />
          </ListItemIcon>
          <ListItemText id="staffing" primary="I require staffing access as my role includes planning"/>
        </ListItem>
        <ListItem key={'line-manager'}>
          <FormControl fullWidth>
            <TextField
              id="outlined-helperText"
              label="Line manager's email address"
              helperText="Optional (this may be helpful if we need to query your request)"
              variant="outlined"
              onChange={event => setState(handleLineManagerChange(state, event.target.value))}
            />
          </FormControl>
        </ListItem>

      </List>
      <ListItem>
        <StyledPaper>
          <StyledTypography>Declaration</StyledTypography>
          <Typography>I understand that:</Typography>
          <DeclarationUl>
            <li>data contained in DRT is marked as OFFICIAL-SENSITIVE</li>
          </DeclarationUl>
          <Typography>I confirm that:</Typography>
          <DeclarationUl>
            <li>I will not share any DRT data with any third party</li>
            <li>I will contact the DRT team at <a href="mailto:props.teamEmail">{props.teamEmail}</a> if I'm asked to
              share any data
            </li>
          </DeclarationUl>
        </StyledPaper>
      </ListItem>
      <ListItem
        button
        key={'agreeDeclaration'}
        onClick={() => setState({...state, agreeDeclaration: !state.agreeDeclaration})}>
        <ListItemIcon>
          <Checkbox
            inputProps={{'aria-labelledby': "agreeDeclaration"}}
            name="agreeDeclaration"
            checked={state.agreeDeclaration}
          />
        </ListItemIcon>
        <ListItemText id="agreeDeclaration" primary="I understand and agree with the above declarations"/>
      </ListItem>

      <Button disabled={(state.portsRequested.length === 0 && !state.allPorts) || !state.agreeDeclaration}
              onClick={save}
              variant="contained"
              color="primary">
        Request access
      </Button>
    </div>;
  }

  return state.requestSubmitted ?
    <ThankYouBox>
      <StyledPaper>
        <h1>Thank you</h1>
        <p>You'll be notified by email when your request has been processed. This usually happens within a couple of
          hours, but may take longer outside core working hours (Monday to Friday, 9am to 5pm).</p>
      </StyledPaper>
    </ThankYouBox> :
    form(props.ports);
}
