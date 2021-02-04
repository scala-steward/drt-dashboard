import React from "react";
import ApiClient from "../services/ApiClient";
import axios, {AxiosResponse} from "axios";
import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import Checkbox from "@material-ui/core/Checkbox";
import ListItemText from "@material-ui/core/ListItemText";
import Divider from "@material-ui/core/Divider";
import {Box, Card, FormControl, Paper, TextField, Typography} from "@material-ui/core";
import Button from "@material-ui/core/Button";
import {createStyles, makeStyles, Theme} from "@material-ui/core/styles";


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

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        container: {
            textAlign: "left",
            marginLeft: theme.spacing(2),
            marginRight: theme.spacing(2),
            padding: theme.spacing(2)
        },
        subHeading: {
            fontWeight: "bold"
        },
        declaration: {
            ...theme.typography.body1,
            listStyleType: "circle"
        },
        thanks: {
            width: "75%"
        }

    }),
);

export default function AccessRequestForm(props: IProps) {
    const apiClient = new ApiClient;

    const classes = useStyles();

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

    const save = () => axios.post(apiClient.requestAccessEndPoint, state)
        .then(setRequestFinished)
        .then(() => axios.get(apiClient.logoutEndPoint))
        .then(() => console.log("User has been logged out."))


    function form(portsAvailable: string[]) {
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
                {state.allPorts ? null : portsAvailable.map((portCode) => {
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
                <Paper className={classes.container}>
                    <Typography className={classes.subHeading}>Declaration</Typography>
                    <Typography>I understand that:</Typography>
                    <ul className={classes.declaration}>
                        <li>all data is provided by third parties, and is therefore OFFICIAL SENSITIVE</li>
                        <li>I should not share any of the data in DRT under any circumstances</li>
                    </ul>
                    <Typography>I confirm that:</Typography>
                    <ul className={classes.declaration}>
                        <li>if I'm asked to share any data I will contact the DRT service team at <a>{props.teamEmail}</a></li>
                    </ul>
                </Paper>
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
        <Box className={classes.thanks}>
        <Paper className={classes.container}>
            <h1>Thank you</h1>
            <p>You'll be notified by email when your request has been processed. This usually happens within a couple of
                hours, but may take longer outside core working hours (Monday to Friday, 9am to 5pm).</p>
        </Paper>
        </Box>
        :
        form(props.ports);


}
