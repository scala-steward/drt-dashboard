import React from "react";
import {styled} from '@mui/material/styles';
import moment from "moment-timezone";
import axios from "axios";
import {
    Box,
    Button,
    FormControl,
    FormGroup,
    InputLabel,
    MenuItem,
    Select,
    TextField
} from "@mui/material";
import {Alert} from "./ViewAlerts"
import {UserProfile} from "../../model/User";
import ApiClient from "../../services/ApiClient";
import {Moment} from "moment/moment";
import {PortsByRegionCheckboxes} from "../PortsByRegionCheckboxes";
import {PortRegion} from "../../model/Config";


const StyledFormControl = styled(FormControl)(({theme}) => ({
    margin: theme.spacing(1),
}));

const SaveButton = styled(Button)(() => ({
    marginTop: 10
}));

interface IProps {
    regions: PortRegion[]
    user: UserProfile
    callback: () => void
}

export default function AlertForm(props: IProps) {
    const [alertPorts, setAlertPorts] = React.useState<string[]>(props.user.ports)
    const [alertClass, setAlertClass] = React.useState<string>('notice')
    const [title, setTitle] = React.useState<string>('')
    const [message, setMessage] = React.useState<string>('')
    const [expires, setExpires] = React.useState<Moment>(moment().add(3, "hours"))

    const save = () => axios.post(ApiClient.alertsEndPoint, {
        alertPorts: alertPorts,
        alertClass: alertClass,
        title: title,
        message: message,
        expires: expires
    }).then(props.callback)

    return (
        <StyledFormControl>
            <FormControl variant="standard">
                <PortsByRegionCheckboxes portDisabled={false}
                                         onSelectedPortsChange={(ports: string[]) => setAlertPorts(ports)}
                                         regions={props.regions}
                                         selectedPorts={alertPorts}
                />
            </FormControl>
            <FormControl variant="standard">
                <InputLabel id="demo-simple-select-label">Alert type</InputLabel>
                <Select
                    labelId="demo-simple-select-label"
                    id="demo-simple-select"
                    value={alertClass}
                    name={"type"}
                    onChange={e => setAlertClass(e.target.value)}
                >
                    <MenuItem value={"notice"}>Notice</MenuItem>
                    <MenuItem value={"warning"}>Warning</MenuItem>

                </Select>
            </FormControl>
            <FormGroup>
                <TextField
                    variant="standard"
                    id="title"
                    label="Title"
                    name="title"
                    InputLabelProps={{
                        shrink: true,
                    }}
                    onChange={e => setTitle(e.target.value)}
                />
            </FormGroup>
            <FormGroup>
                <TextField
                    variant="standard"
                    id="message"
                    label="Message"
                    name="message"
                    rows={2}
                    fullWidth={true}
                    InputLabelProps={{
                        shrink: true,
                    }}
                    multiline
                    onChange={e => setMessage(e.target.value)}
                />
            </FormGroup>
            <FormGroup>
                <TextField
                    variant="standard"
                    id="expires"
                    label="Expires"
                    type="datetime-local"
                    defaultValue={expires}
                    InputLabelProps={{
                        shrink: true,
                    }}
                    name={"expires"}
                    onChange={e => setExpires(moment(e.target.value))}
                />
            </FormGroup>
            <FormGroup>
                <SaveButton variant="contained" color="primary" onClick={save}>Save</SaveButton>
            </FormGroup>

            <Box m={2}><Alert alertClass={alertClass} title={title} message={message}
                              expires={expires.valueOf()}/></Box>
        </StyledFormControl>
    );
}
