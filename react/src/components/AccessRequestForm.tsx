import React from "react";
import {styled} from '@mui/material/styles';
import ApiClient from "../services/ApiClient";
import axios from "axios";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Checkbox from "@mui/material/Checkbox";
import ListItemText from "@mui/material/ListItemText";
import {Box, Button, Divider, FormControl, Typography} from "@mui/material";
import {PortRegion, PortRegionHelper} from "../model/Config";
import {PortsByRegionCheckboxes} from "./PortsByRegionCheckboxes";
import InitialRequestForm from "./InitialRequestForm";
import AccessRequestAdditionalInformationForm from "./AccessRequestAdditionalInformationForm";
import _ from "lodash/fp";
// @ts-ignore
import isEmail from "validator/lib/isEmail";
import InputLabel from '@mui/material/InputLabel';
import OutlinedInput from "@mui/material/OutlinedInput";

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
  teamEmail: string;
}

interface AccessRequest {
  agreeDeclaration: boolean;
  allPorts: boolean;
  lineManager: string;
  portOrRegionText: string;
  portsRequested: string[];
  rccOption: string;
  regionsRequested: string[];
  staffing: boolean;
  staffText: string;
}

export default function AccessRequestForm(props: IProps) {
  const [selectedPorts, setSelectedPorts]: [string[], ((value: (((prevState: string[]) => string[]) | string[])) => void)] = React.useState<string[]>([])
  const [portOrRegionText, setPortOrRegionText]: [string, ((value: (((prevState: string) => string) | string)) => void)] = React.useState<string>("")
  const [staffText, setStaffText]: [string, ((value: (((prevState: string) => string) | string)) => void)] = React.useState<string>("")
  const [isValid, setIsValid] = React.useState(false);
  const [dirty, setDirty] = React.useState(false);
  const [openModal, setOpenModal]: [boolean, ((value: (((prevState: boolean) => boolean) | boolean)) => void)] = React.useState<boolean>(false);

  const selectedRegions = props.regions.filter(region => region.ports.every(port => selectedPorts.includes(port)))

  const [staffingSelected, setStaffingSelected] = React.useState<boolean>(false)
  const [lineManager, setLineManager] = React.useState<string>('')
  const [declarationAgreed, setDeclarationAgreed] = React.useState<boolean>(false)
  const [requestSubmitted, setRequestSubmitted] = React.useState<boolean>(false)
  const [isRccUser, setIsRccUser] = React.useState<boolean>(false)

  const handleRccOption = (isRccUser: boolean) => {
    setIsRccUser(isRccUser)
    setSelectedPorts([])
    setPortOrRegionText("")
    setStaffText("")
  }

  const save = () => {
    const allPortsRequested = _.isEmpty(_.xor(selectedPorts, PortRegionHelper.portsInRegions(props.regions)))
    axios.post(ApiClient.requestAccessEndPoint, {
      agreeDeclaration: declarationAgreed,
      allPorts: allPortsRequested,
      lineManager: lineManager,
      portOrRegionText: portOrRegionText,
      portsRequested: selectedPorts,
      rccOption: isRccUser ? 'rccu' : 'port',
      regionsRequested: selectedRegions.map(r => r.name),
      staffing: staffingSelected,
      staffText: staffText,
    } as AccessRequest)
      .then(() => setRequestSubmitted(true))
      .then(() => axios.get(ApiClient.logoutEndPoint))
      .then(() => console.log("User has been logged out."))
  }

  const pageMessage = () => {
    if (isRccUser)
      return "Please select the RCCU region you require access to"
    else
      return "Please select the ports you require access to"
  }

  const moreInfoRequired = () => {
    return (((selectedPorts.length > 1 && !isRccUser) ||
      (selectedPorts.length > 0 && !isRccUser && staffingSelected) ||
      (selectedRegions.length > 1 && isRccUser) ||
      (selectedRegions.length > 0 && isRccUser && staffingSelected)))
  }

  const enableRequestForModal = () => {
    return (moreInfoRequired() && isValid && declarationAgreed) ||
      (((selectedPorts.length === 1 && !isRccUser) ||
          (selectedRegions.length === 1 && isRccUser)) &&
        declarationAgreed && !staffingSelected)
  }

  const singlePortOrRegion = () => {
    return (((selectedPorts.length === 1 && !isRccUser) ||
        (selectedRegions.length === 1 && isRccUser)) &&
      declarationAgreed && !staffingSelected)
  }

  const saveOrModal = () => {
    if (singlePortOrRegion()) {
      save()
    } else {
      setOpenModal(true);
    }
  }

  const handleEmailChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (isEmail(event.target.value)) {
      setIsValid(true);
    } else {
      setIsValid(false);
    }
    setLineManager(event.target.value);
  };

  function form() {
    return <Box sx={{width: '100%'}}>
      <h1>Welcome to DRT</h1>
      <InitialRequestForm handleRccOptionCallback={handleRccOption}/>
      <Divider/>
      <p>{pageMessage()}</p>
      <List>
        <ListItem>
          {isRccUser ?
            <PortsByRegionCheckboxes portDisabled={true}
                                     regions={props.regions}
                                     selectedPorts={selectedPorts}
                                     onSelectedPortsChange={(ports: string[]) => setSelectedPorts(ports)}/> :
            <PortsByRegionCheckboxes portDisabled={false}
                                     regions={props.regions}
                                     selectedPorts={selectedPorts}
                                     onSelectedPortsChange={(ports: string[]) => setSelectedPorts(ports)}/>
          }
        </ListItem>
        <Divider/>
        <ListItem
          button
          key={'staffing'}
        >
          <ListItemIcon>
            <Checkbox
              inputProps={{'aria-labelledby': "staffing"}}
              name="staffing"
              checked={staffingSelected}
              onChange={event => setStaffingSelected(event.target.checked)}
            />
          </ListItemIcon>
          <ListItemText id="staffing"
                        primary="I require access to enter staffing figures as my role includes planning"/>
        </ListItem>
        <ListItem key={'line-manager'}>
          <FormControl fullWidth>
            <InputLabel error={moreInfoRequired() && !isValid} htmlFor="component-outlined">Line manager's
              email address</InputLabel>
            <OutlinedInput
              id="component-outlined"
              error={dirty && !isValid}
              onBlur={() => setDirty(true)}
              onChange={handleEmailChange}
              label="Line manager's email address"
              size={'medium'}
              value={lineManager}
            />
          </FormControl>
        </ListItem>
        <Divider/>
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
              <li>I will contact the DRT team at <a href="mailto:props.teamEmail">{props.teamEmail}</a> if
                I'm asked to share any data
              </li>
            </DeclarationUl>
          </Declaration>
        </ListItem>
        <ListItem
          button
          key={'agreeDeclaration'}
        >
          <ListItemIcon>
            <Checkbox
              inputProps={{'aria-labelledby': "agreeDeclaration"}}
              name="agreeDeclaration"
              checked={declarationAgreed}
              onChange={event => setDeclarationAgreed(event.target.checked)}
            />
          </ListItemIcon>
          <ListItemText id="agreeDeclaration" primary="I understand and agree with the above declarations"/>
        </ListItem>
        {(openModal) ? <AccessRequestAdditionalInformationForm openModal={openModal}
                                                               setOpenModal={setOpenModal}
                                                               rccOption={isRccUser}
                                                               rccRegions={selectedRegions.map(r => r.name)}
                                                               ports={selectedPorts}
                                                               manageStaff={staffingSelected}
                                                               portOrRegionText={portOrRegionText}
                                                               setPortOrRegionText={setPortOrRegionText}
                                                               staffText={staffText}
                                                               setStaffText={setStaffText}
                                                               saveCallback={save}/> : <span/>
        }
        <Button
          disabled={!enableRequestForModal()}
          onClick={saveOrModal}
          variant="contained"
          color="primary"
        > Request access
        </Button>
      </List>
    </Box>;
  }

  return requestSubmitted ?
    <ThankYouBox>
      <Declaration>
        <h1>Thank you</h1>
        <p>You'll be notified by email when your request has been processed. This usually happens within a
          couple of hours, but may take longer outside core working hours (Monday to Friday, 9am to 5pm).</p>
      </Declaration>
    </ThankYouBox> :
    form();
}
