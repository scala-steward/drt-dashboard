import React from "react";
import {styled} from '@mui/material/styles';
import ApiClient from "../services/ApiClient";
import axios from "axios";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import Checkbox from "@mui/material/Checkbox";
import {RadioGroup, Radio} from "@mui/material";
import {Box, Button, Divider, FormControl, FormLabel, FormControlLabel, Typography} from "@mui/material";
import {PortRegion, PortRegionHelper} from "../model/Config";
import {PortsByRegionCheckboxes} from "./PortsByRegionCheckboxes";
import InitialRequestForm from "./InitialRequestForm";
import AccessRequestAdditionalInformationForm from "./AccessRequestAdditionalInformationForm";
import _ from "lodash/fp";
// @ts-ignore
import isEmail from "validator/lib/isEmail";
import InputLabel from '@mui/material/InputLabel';
import OutlinedInput from "@mui/material/OutlinedInput";
import {equals} from "validator";

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
  const [radioSelected, setRadioSelected] = React.useState<boolean>(false);
  const [staffingSelectedRadio, setStaffingSelectedRadio] = React.useState<string>("");

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
      lineManager: staffingSelected? lineManager : "",
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
      (selectedPorts.length > 0 && !isRccUser) ||
      (selectedRegions.length > 1 && isRccUser) ||
      (selectedRegions.length > 0 && isRccUser)))
  }

  const enableRequestForModal = () => {
    return (moreInfoRequired() && inputIsValid(staffingSelected, lineManager)
            && declarationAgreed && radioSelected)
  }

  const singlePortOrRegion = () => {
    return (((selectedPorts.length === 1 && !isRccUser) ||
        (selectedRegions.length === 1 && isRccUser)) &&
      declarationAgreed)
  }

  const saveOrModal = () => {
    if (singlePortOrRegion()) {
      save()
    } else {
      setOpenModal(true);
    }
  }

  const inputIsValid = (staffingSelectedBool: boolean, lineManager: string) => {
    if(staffingSelectedBool || selectedPorts.length > 1) {
      return isEmail(lineManager);
    } else {
      return true;
    }
  }

  const handleEmailChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setIsValid(inputIsValid(staffingSelected, event.target.value))
    setLineManager(event.target.value);
  };

  const handleRadioChange = (event: React.ChangeEvent<HTMLInputElement>, staffingSelectedRadio: string) => {
    const staffingSelectedBool = equals(staffingSelectedRadio, "true")
    setIsValid(inputIsValid(staffingSelectedBool, lineManager))
    setDirty(false);
    setStaffingSelected(staffingSelectedBool);
    setRadioSelected(true);
    setStaffingSelectedRadio(staffingSelectedRadio);
  }

  function form() {
    return <Box sx={{width: '100%'}}>
      <h1>Welcome to DRT</h1>
      <InitialRequestForm handleRccOptionCallback={handleRccOption}/>
      <Divider/>
      <p>{pageMessage()}</p>
      <List>
        <ListItem>
          <PortsByRegionCheckboxes portDisabled={isRccUser}
                                   regions={props.regions}
                                   selectedPorts={selectedPorts}
                                   onSelectedPortsChange={(ports: string[]) => setSelectedPorts(ports)}
          />
        </ListItem>
        <Divider/>
        <ListItem>
          <FormLabel sx={{width: '100%'}}
              id="staffing-selected-group-label">
            <br/>
            Do you work in your port or command level planning team? <br/>
            <b>[hint] Select yes if you need to enter staffing</b>
          </FormLabel>
        </ListItem>
        <ListItem>
        <FormControl sx={{width: '100%', paddingBottom: '1em'}}>
          <RadioGroup
              aria-labelledby="staffing-selected-group-label"
              defaultValue=""
              name="staffing-selected-group"
              onChange={handleRadioChange}
              value={staffingSelectedRadio}>
            <FormControlLabel value="true" control={<Radio/>} label="Yes"/>
            <FormControlLabel value="false" control={<Radio/>} label="No"/>
          </RadioGroup>
          {
            (selectedPorts.length > 1 || staffingSelected) &&
          <FormControl fullWidth>
            <InputLabel error={dirty && !isValid} htmlFor="line-manager-email-input">Enter your line manager's
              email address</InputLabel>
            <OutlinedInput
                id="line-manager-email-input"
                inputProps={{ "data-testid":  "line-manager-email-input-test" }}
                onBlur={() => setDirty(selectedPorts.length > 1 || staffingSelected)}
                onChange={handleEmailChange}
                label="Line manager's email address"
                size={'medium'}
                value={lineManager}
            />
          </FormControl>
          }
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
          <FormControlLabel
            control={<Checkbox
              inputProps={{'aria-labelledby': "agreeDeclaration"}}
              name="agreeDeclaration"
              checked={declarationAgreed}
              onChange={event => setDeclarationAgreed(event.target.checked)}
            />}
            label="I understand and agree with the above declarations"
            sx={{fontWeight: 'bold'}}
          />
        </ListItem>
        {(openModal) ? <AccessRequestAdditionalInformationForm openModal={openModal}
                                                               setOpenModal={setOpenModal}
                                                               rccOption={isRccUser}
                                                               rccRegions={selectedRegions.map(r => r.name)}
                                                               ports={selectedPorts}
                                                               portOrRegionText={portOrRegionText}
                                                               setPortOrRegionText={setPortOrRegionText}
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
