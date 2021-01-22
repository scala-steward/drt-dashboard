import React from "react";
import UserLike from "../model/User";
import {
    Button,
    Checkbox,
    Container,
    FormControl,
    FormControlLabel,
    FormGroup,
    FormLabel,
    InputLabel,
    MenuItem,
    Select,
    TextField,
    Typography
} from "@material-ui/core";

import {createStyles, makeStyles, Theme} from '@material-ui/core/styles';
import axios from "axios";
import moment from "moment-timezone";
moment.locale("en-gb");

interface IProps {
    user: UserLike;
}

interface IPortSelection {
    [key: string]: boolean
}

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        root: {
            '& .MuiFormControl-root': {
                margin: theme.spacing(1),
                width: '60ch',
            },
        },
        select: {
            '& .MuiInputBase-input': {
                textAlign: "left"

            },
        }
    }),
);

export default function Alerts(props: IProps) {

    const classes = useStyles();

    const [state, setState] = React.useState(
        {
            alertPorts: props.user.ports.reduce(
                (acc: IPortSelection, curr: string) => {
                    acc[curr] = true
                    return acc
                },
                {}
            ),
            alertClass: "notice",
            title: "",
            message: "",
            expires: moment().add(3, "hours").format("YYYY-MM-DDTHH:mm")
        });

    const updatePortSelection = (event: React.ChangeEvent<HTMLInputElement>) =>

        setState({
            ...state,
            alertPorts: {
                ...state.alertPorts,
                [event.target.name]: event.target.checked
            }
        });
    const updateField = (event: React.ChangeEvent<HTMLInputElement>) => setState({
        ...state,
        [event.target.name]: event.target.value
    })

    const handleChange = (event: React.ChangeEvent<{ value: unknown }>) => {
        setState({
            ...state,
            alertClass: event.target.value as string
        })
    };

    const markdown = (state.title != "" ? "**" + state.title + "**" : "") +
        (state.message !== "" ? " - " + state.message : "")

    const save = () => axios.post("/api/alerts", state)


    return (<Container>

        <Typography variant={"h4"}>Alerts</Typography>
        <FormControl className={classes.root} component="fieldset">
            <FormLabel component="legend">Select ports</FormLabel>
            <FormGroup>
                {props.user.ports.map((portCode) => {

                    return <FormControlLabel
                        key={portCode}
                        control={<Checkbox defaultChecked={state.alertPorts[portCode] || false}
                                           onChange={updatePortSelection} name={portCode}/>}
                        label={portCode}
                    />
                })
                }
            </FormGroup>
            <FormControl>
                <InputLabel id="demo-simple-select-label">Alert type</InputLabel>
                <Select
                    className={classes.select}
                    labelId="demo-simple-select-label"
                    id="demo-simple-select"
                    value={state.alertClass || "notice"}
                    name={"type"}
                    onChange={handleChange}
                >
                    <MenuItem value={"notice"}>Notice</MenuItem>
                    <MenuItem value={"warning"}>Warning</MenuItem>

                </Select>
            </FormControl>
            <FormGroup>
                <TextField
                    id="title"
                    label="Title"
                    name="title"
                    InputLabelProps={{
                        shrink: true,
                    }}
                    onChange={updateField}
                />
            </FormGroup>
            <FormGroup>
                <TextField
                    id="message"
                    label="Message"
                    name="message"
                    onChange={updateField}
                    rows={2}
                    fullWidth={true}
                    InputLabelProps={{
                        shrink: true,
                    }}
                    multiline
                />
            </FormGroup>
            <FormGroup>
                <TextField
                    id="expires"
                    label="Expires"
                    type="datetime-local"
                    defaultValue={state.expires}
                    InputLabelProps={{
                        shrink: true,
                    }}
                    name={"expires"}
                    onChange={updateField}
                />
            </FormGroup>
            <FormGroup>
                <Button variant="contained" color="primary" onClick={save}>
                    Save
                </Button>
            </FormGroup>
        </FormControl>


    </Container>);
}
