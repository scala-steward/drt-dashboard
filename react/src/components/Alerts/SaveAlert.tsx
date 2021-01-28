import React from "react";
import moment from "moment-timezone";
import UserLike from "../../model/User";
import {createStyles, makeStyles, Theme} from "@material-ui/core/styles";
import axios from "axios";
import {
    Box,
    Button,
    Checkbox,
    FormControl,
    FormControlLabel,
    FormGroup,
    InputLabel,
    MenuItem,
    Select,
    TextField
} from "@material-ui/core";
import {Alert} from "./ViewAlerts"
import AlertLike from "../../model/Alert";

interface IProps {
    user: UserLike
    callback: () => void
}

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        form: {
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

interface IPortSelection {
    [key: string]: boolean
}

export default function SaveAlert(props: IProps) {
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

    const updateField = (event: React.ChangeEvent<HTMLInputElement>) =>
        setState({
            ...state,
            [event.target.name]: event.target.value
        })

    const handleChange = (event: React.ChangeEvent<{ value: unknown }>) =>
        setState({
            ...state,
            alertClass: event.target.value as string
        });

    const theAlert: AlertLike = {
        title: state.title,
        message: state.message,
        expires: moment(state.expires).unix(),
        alertClass: state.alertClass
    }

    const save = () => axios.post("/api/alerts", state).then(props.callback)

    return (<FormControl className={classes.form} component="fieldset">
        <FormGroup>
            {
                props.user.ports.map((portCode) => {
                    return <FormControlLabel
                        key={portCode}
                        control={
                            <Checkbox
                                defaultChecked={state.alertPorts[portCode] || false}
                                onChange={updatePortSelection} name={portCode}
                            />
                        }
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
                rows={2}
                fullWidth={true}
                InputLabelProps={{
                    shrink: true,
                }}
                multiline
                onChange={updateField}
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

        <Box m={2}><Alert alert={theAlert}/></Box>
    </FormControl>)
}
