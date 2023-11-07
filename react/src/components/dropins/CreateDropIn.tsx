import React, {useState} from 'react';
import axios, {AxiosResponse} from "axios";
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider';
import {DateTimePicker} from '@mui/x-date-pickers/DateTimePicker';
import TextField from '@mui/material/TextField';
import Button from "@mui/material/Button";
import Box from "@mui/material/Box";
import Grid from "@mui/material/Grid";
import moment from 'moment-timezone';
import {AdapterMoment} from "@mui/x-date-pickers/AdapterMoment";
import {Moment} from "moment";
import {Snackbar} from "@mui/material";
import {Alert} from "../DialogComponent";
import {redirect} from "react-router-dom";

export function jsonDropInData(startTime: Moment | null, endTime: Moment | null, title: string, meetingLink: string) {
  const startTimeString = startTime?.format()
  const endTimeString = endTime?.format()
  return {
    'title': title,
    'startTime': startTimeString,
    'endTime': endTimeString,
    'meetingLink': meetingLink
  }
}

export function CreateDropIn() {
  moment.tz.setDefault('Europe/London');
  const [error, setError] = useState(false);
  const [errorText, setErrorText] = useState('');
  const [title, setTitle] = useState('');
  const [startTime, setStartTime] = React.useState<Moment | null>(null);
  const [endTime, setEndTime] = React.useState<Moment | null>(null);
  const [meetingLink, setMeetingLink] = useState('');
  const [formSubmitted, setFormSubmitted] = useState(false);
  const [hasFocusStartTime, setHasFocusStartTime] = useState(false);
  const [hasFocusEndTime, setHasFocusEndTime] = useState(false);

  const handleResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      redirect('/drop-ins/list/crud/saved');
      response.data
    } else {
      setError(true);
      response.data
    }
  }

  const validateForm = () => {
    return title && startTime && endTime && endTime.isAfter(startTime)
  }

  const handleSubmit = (e: React.FormEvent) => {
    if (validateForm()) {
      setFormSubmitted(true);
      e.preventDefault();
      axios.post('/drop-in/save', jsonDropInData(startTime, endTime, title, meetingLink))
        .then(response => handleResponse(response))
        .then(data => {
          console.log(data);
        })
        .catch(error => {
          setError(true);
          setErrorText('There was a problem saving the drop-in guide. Please try again later.');
          console.error(error);
        });
    } else {
      e.preventDefault();
      setFormSubmitted(true);
      setError(true);
      setErrorText('Please fill in all required fields. End time greater than start time.');
    }

  };

  return (
    <div>
      <Box>
        <h1>Create Drop-In</h1>
        <Snackbar
          anchorOrigin={{vertical: 'top', horizontal: 'center'}}
          open={error}
          autoHideDuration={6000}
          onClose={() => setError(false)}>
          <Alert onClose={() => setError(false)} severity="error" sx={{width: '100%'}}>
            {errorText}
          </Alert>
        </Snackbar>
        <form onSubmit={handleSubmit}>
          <Grid container spacing={3} alignItems="center">
            <Grid item xs={12}>
              <TextField required
                         label="Title" type="text" value={title}
                         error={formSubmitted && !title}
                         helperText={formSubmitted && !title ? "Title is required" : ""}
                         onChange={(e) => setTitle(e.target.value)}/>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Meeting Link"
                sx={{width: 400}}
                variant="outlined"
                placeholder="Copy and paste your meeting link here"
                value={meetingLink}
                onChange={(e) => setMeetingLink(e.target.value)}/>
            </Grid>
            <Grid item xs={12}>
              <LocalizationProvider dateAdapter={AdapterMoment}>
                {hasFocusStartTime || startTime ? <DateTimePicker
                  slotProps={{
                    textField: {
                      variant: 'outlined',
                      required: true,
                      error: formSubmitted && !startTime,
                      helperText: (formSubmitted && !startTime) ? "StartTime is required" : ""
                    }
                  }}
                  label="Start Time"
                  value={startTime}
                  format="DD/MM/YYYY HH:mm A"
                  onChange={(newValue) => {
                    setStartTime(newValue);
                    setEndTime(newValue);
                  }}
                /> : (
                  <TextField
                    label="Start Time"
                    onClick={() => setHasFocusStartTime(true)}
                    error={formSubmitted && !startTime}
                    helperText={formSubmitted && !startTime ? "Start time is required" : ""}
                  />
                )}
              </LocalizationProvider>
            </Grid>
            <Grid item xs={12}>
              <LocalizationProvider dateAdapter={AdapterMoment}>
                {hasFocusEndTime || endTime ? <DateTimePicker
                  slotProps={{
                    textField: {
                      variant: 'outlined',
                      required: true,
                      error: formSubmitted && !endTime,
                      helperText: (formSubmitted && !endTime) ? "End time is required" : ""
                    }
                  }}
                  label="End Time"
                  value={endTime}
                  format="DD/MM/YYYY HH:mm A"
                  onChange={(newValue) => {
                    setEndTime(newValue);
                  }}
                /> : (
                  <TextField
                    label="End Time"
                    onClick={() => setHasFocusEndTime(true)}
                    error={formSubmitted && !endTime}
                    helperText={formSubmitted && !endTime ? "EndTime is required" : ""}
                  />
                )}
              </LocalizationProvider>
            </Grid>
            <Grid item xs={12}>
              <Box sx={{alignItems: "left"}}>
                <Button variant="outlined" type="submit">Submit</Button>
              </Box>
            </Grid>
          </Grid>
        </form>
      </Box>
    </div>
  );
}
