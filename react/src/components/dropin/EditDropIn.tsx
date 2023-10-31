import React, {useEffect, useState} from "react";
import {Button, Snackbar} from "@mui/material";
import axios, {AxiosResponse} from "axios";
import Grid from "@mui/material/Grid";
import TextField from "@mui/material/TextField";
import {LocalizationProvider} from "@mui/x-date-pickers/LocalizationProvider";
import {DateTimePicker} from "@mui/x-date-pickers/DateTimePicker";
import Box from "@mui/material/Box";
import moment, {Moment} from 'moment';
import {AdapterMoment} from "@mui/x-date-pickers/AdapterMoment";
import {jsonDropInData} from "./CreateDropIn";
import {useParams} from 'react-router-dom';
import {Redirect} from 'react-router-dom';
import {Alert} from "../DialogComponent";

export function EditDropIn() {
    const [redirectTo, setRedirectTo] = useState('');
    const {dropInId} = useParams<{ dropInId: string }>();
    const [editTitle, setEditTitle] = React.useState<string>('')
    const [editStartTime, setEditStartTime] = React.useState<Moment | null>(null);
    const [editEndTime, setEditEndTime] = React.useState<Moment | null>(null);

    const [editMeetingLink, setEditMeetingLink] = React.useState<string>('');
    const [error, setError] = useState(false);

    const handleResponse = (response: AxiosResponse) => {
        if (response.status === 200) {
            setEditTitle(response.data.title)
            setEditStartTime(moment(response.data.startTime))
            setEditEndTime(moment(response.data.endTime))
            setEditMeetingLink(response.data.meetingLink)
        } else {
            setError(true);
        }
    }

    const handleUpdateResponse = (response: AxiosResponse) => {
        if (response.status === 200) {
            setRedirectTo('/drop-ins/list/crud/edited');
            response.data
        } else {
            setError(true);
            response.data
        }
    }

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        axios.put('/drop-in/update/' + dropInId,
            jsonDropInData(editStartTime, editEndTime, editTitle || '', editMeetingLink || ''))
            .then(response => handleUpdateResponse(response))
            .then(data => {
                console.log(data);
            })
            .catch(error => {
                setError(true);
                console.error(error);
            });
    };

    useEffect(() => {
        axios.get('/drop-in/get/' + dropInId)
            .then(response => handleResponse(response))
            .then(data => {
                console.log(data);
            }).catch(error => {
            setError(true);
            console.error(error);
        });
    }, [dropInId]);

    return (
        <div>
            {redirectTo && <Redirect to={redirectTo}/>}
            {<div>
                <h1>Edit Drop-In</h1>
                <Snackbar
                    anchorOrigin={{vertical: 'top', horizontal: 'center'}}
                    open={error}
                    autoHideDuration={6000}
                    onClose={() => setError(false)}>
                    <Alert onClose={() => setError(false)} severity="error" sx={{width: '100%'}}>
                        There was a problem saving the drop-in guide.
                    </Alert>
                </Snackbar>
                <form onSubmit={handleSubmit}>
                    <Grid container spacing={3} alignItems="center">
                        <Grid item xs={12}>
                            <TextField label="Title" type="text" value={editTitle}
                                       onChange={(e) => setEditTitle(e.target.value)}/>
                        </Grid>
                        <Grid item xs={12}>
                            <TextField
                                fullWidth
                                label="Meeting Link"
                                sx={{width: 400}}
                                multiline
                                rows={1}
                                variant="outlined"
                                placeholder="Paste your meeting link here"
                                value={editMeetingLink}
                                onChange={(e) => setEditMeetingLink(e.target.value)}/>
                        </Grid>
                        <Grid item xs={12}>
                            <LocalizationProvider dateAdapter={AdapterMoment}>
                                <DateTimePicker
                                    renderInput={(props) => <TextField {...props} />}
                                    label="Start Time"
                                    value={editStartTime}
                                    inputFormat="DD/MM/YYYY HH:mm A"
                                    onChange={(newValue) => {
                                        setEditStartTime(newValue);
                                    }}
                                />
                            </LocalizationProvider>
                        </Grid>
                        <Grid item xs={12}>
                            <LocalizationProvider dateAdapter={AdapterMoment}>
                                <DateTimePicker
                                    renderInput={(props) => <TextField {...props} />}
                                    label="End Time"
                                    value={editEndTime}
                                    inputFormat="DD/MM/YYYY HH:mm A"
                                    onChange={(newValue) => {
                                        setEditEndTime(newValue);
                                    }}
                                />
                            </LocalizationProvider>
                        </Grid>
                        <Grid item xs={12}>
                            <Box sx={{alignItems: "left"}}>
                                <Button variant="outlined" type="submit">Update</Button>
                            </Box>
                        </Grid>
                    </Grid>
                </form>
            </div>}</div>
    )
}
