import React from "react";
import {styled} from '@mui/material/styles';
import ApiClient from "../services/ApiClient";
import axios from "axios";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Checkbox from "@mui/material/Checkbox";
import ListItemText from "@mui/material/ListItemText";
import {Box, Button, Divider, FormControl, TextField, Typography} from "@mui/material";
import {PortRegion} from "../model/Config";


const Declaration = styled('div')(({theme}) => ({
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
  regions: PortRegion[];
  ports: string[];
  teamEmail: string;
}

interface IState {
  portsRequested: string[];
  staffing: boolean;
  lineManager: string;
  agreeDeclaration: boolean;
  requestSubmitted: boolean;
}

export default function AccessRequestForm(props: IProps) {
  const [state, setState] = React.useState(
    {
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

  const allPorts = props.regions.map(r => r.ports).reduce((a, b) => a.concat(b))
  const allPortsCount = props.regions.reduce((a, b) => a + b.ports.length, 0)

  function form() {
    const allPortsSelected = state.portsRequested.length === allPortsCount
    return <Box sx={{width: '100%'}}>
      <h1>Welcome to DRT</h1>
      <p>Please select the ports you require access to</p>
      <List>
        <ListItem
          button
          key={'allPorts'}
          onClick={() => {
            if (!allPortsSelected) setState({...state, portsRequested: allPorts})
            else setState({...state, portsRequested: []})
          }}>
          <ListItemIcon>
            <Checkbox
              inputProps={{'aria-labelledby': "allPorts"}}
              name="allPorts"
              checked={state.portsRequested.length === allPortsCount}
              indeterminate={state.portsRequested.length > 0 && state.portsRequested.length < allPortsCount}
            />
          </ListItemIcon>
          <ListItemText id="allPorts">
            <Box sx={{fontWeight: 'bold'}}>All regions</Box>
          </ListItemText>
        </ListItem>
        <ListItem alignItems='flex-start'>
          <Box sx={{
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'space-start',
          }}>
            {props.regions.map((region) => {
              const sortedPorts = [...region.ports].sort()
              const regionSelected = sortedPorts.every(p => state.portsRequested.includes(p))
              const regionPartiallySelected = sortedPorts.some(p => state.portsRequested.includes(p)) && !regionSelected

              function toggleRegionPorts() {
                if (!regionSelected) setState({...state, portsRequested: state.portsRequested.concat(sortedPorts)})
                else setState({...state, portsRequested: state.portsRequested.filter(p => !sortedPorts.includes(p))})
              }

              return <Box sx={{
                minWidth: '200px',
                marginRight: '50px'
              }}>
                <List sx={{verticalAlign: 'top'}}>
                  <ListItem
                    button
                    onClick={() => toggleRegionPorts()}
                    key={region.name}
                    disablePadding={true}
                  >
                    <ListItemIcon>
                      <Checkbox
                        inputProps={{'aria-labelledby': region.name}}
                        name={region.name}
                        checked={regionSelected}
                        indeterminate={regionPartiallySelected}
                      />
                    </ListItemIcon>
                    <ListItemText>
                      <Box sx={{fontWeight: 'bold'}}>{region.name}</Box>
                    </ListItemText>
                  </ListItem>
                  {sortedPorts.map((portCode) => {
                    return <ListItem
                      button
                      key={portCode}
                      onClick={() => updatePortSelection(portCode)}
                      disablePadding={true}
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
                </List>
              </Box>
            })}
          </Box>
        </ListItem>
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
        <Declaration>
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
        </Declaration>
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

      <Button
        disabled={(state.portsRequested.length === 0) || !state.agreeDeclaration}
        onClick={save}
        variant="contained"
        color="primary"
      >
        Request access
      </Button>
    </Box>;
  }

  return state.requestSubmitted ?
    <ThankYouBox>
      <Declaration>
        <h1>Thank you</h1>
        <p>You'll be notified by email when your request has been processed. This usually happens within a couple
          of
          hours, but may take longer outside core working hours (Monday to Friday, 9am to 5pm).</p>
      </Declaration>
    </ThankYouBox> :
    form();
}
