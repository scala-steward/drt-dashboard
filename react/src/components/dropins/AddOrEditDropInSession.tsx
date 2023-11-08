import React, {useEffect, useState} from 'react';
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
import {Breadcrumbs, Stack} from "@mui/material";
import {Alert} from "../DialogComponent";
import {Link, useNavigate, useParams} from "react-router-dom";
import Typography from "@mui/material/Typography";
import {enqueueSnackbar} from "notistack";
import ApiClient from "../../services/ApiClient";

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

export function AddOrEditDropInSession() {
  moment.tz.setDefault('Europe/London')

  const navigate = useNavigate()
  const [errorText, setErrorText] = useState('')
  const [titleError, setTitleError] = useState(false)
  const [startTimeError, setStartTimeError] = useState(false)
  const [endTimeError, setEndTimeError] = useState(false)
  const [title, setTitle] = useState('')
  const [startTime, setStartTime] = React.useState<Moment | null>(null)
  const [endTime, setEndTime] = React.useState<Moment | null>(null)
  const [meetingLink, setMeetingLink] = useState('')

  const params = useParams()
  const sessionId = params['dropInId'] ? params['dropInId'] : ''

  const handleResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      setTitle(response.data.title)
      setStartTime(moment(response.data.startTime))
      setEndTime(moment(response.data.endTime))
      setMeetingLink(response.data.meetingLink)
    } else
      enqueueSnackbar('There was a problem loading the drop-in session. Try refreshing the page.', {variant: 'error'})
  }

  useEffect(() => {
    const fetchSession = () => sessionId && axios.get(`${ApiClient.getDropInSessionEndpoint}/${sessionId}`)
      .then(response => handleResponse(response))
      .catch(error => {
        enqueueSnackbar('There was a problem loading the drop-in session. Try refreshing the page.')
        console.error(error);
      });
    fetchSession()
  }, [])

  const formIsValid = () => {
    if (!title) setTitleError(true)
    else setTitleError(false)
    if (!startTime) setStartTimeError(true)
    else setStartTimeError(false)
    if (!endTime) setEndTimeError(true)
    else setEndTimeError(false)

    return title && startTime && endTime && endTime.isAfter(startTime)
  }

  const handleSubmit = (e: React.FormEvent) => {
    console.log(`title: ${title}, startTime: ${startTime}, endTime: ${endTime}, meetingLink: ${meetingLink}`)
    if (formIsValid()) {
      e.preventDefault();
      const data = jsonDropInData(startTime, endTime, title, meetingLink)
      const submitAction = sessionId ?
        axios.put(`${ApiClient.updateDropInSessionEndpoint}/${sessionId}`, data) :
        axios.post(ApiClient.saveDropInSessionEndpoint, data)

      submitAction
        .then(res => {
          if (res.status === 200) {
            console.log('Drop-in guide saved')
            enqueueSnackbar('Drop-in guide saved', {variant: 'success'})
            navigate('/drop-ins')
          } else
            enqueueSnackbar('There was a problem saving the drop-in guide. Please try again later.')
        })
        .catch(error => {
          setErrorText('There was a problem saving the drop-in guide. Please try again later.');
          console.error(error);
        });
    } else {
      e.preventDefault();
      setErrorText('Please fill in all required fields. Start time must be before the end time.');
    }
  };

  return (
    <Stack sx={{mt: 2, gap: 4, alignItems: 'stretch'}}>
      <Breadcrumbs>
        <Link to={"/"}>
          Home
        </Link>
        <Link to={"/drop-ins"}>
          Drop-in sessions
        </Link>
        <Typography color="text.primary">Add session</Typography>
      </Breadcrumbs>
      {errorText && <Alert severity="warning">{errorText}</Alert>}
      <form onSubmit={handleSubmit}>
        <Grid container spacing={3} alignItems="center">
          <Grid item xs={12}>
            <TextField label="Title *"
                       type="text"
                       value={title}
                       error={titleError}
                       helperText={titleError ? "Title is required" : ""}
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
            <LocalizationProvider dateAdapter={AdapterMoment} adapterLocale={'en-gb'}>
              <DateTimePicker
                slotProps={{
                  textField: {
                    variant: 'outlined',
                    error: startTimeError,
                    helperText: startTimeError ? "StartTime is required" : ""
                  }
                }}
                label="Start Time *"
                value={startTime}
                onChange={(newValue) => {
                  setStartTime(newValue);
                  setEndTime(newValue);
                }}
              />
            </LocalizationProvider>
          </Grid>
          <Grid item xs={12}>
            <LocalizationProvider dateAdapter={AdapterMoment}>
              <DateTimePicker
                slotProps={{
                  textField: {
                    variant: 'outlined',
                    error: endTimeError,
                    helperText: endTimeError ? "End time is required" : ""
                  }
                }}
                label="End Time *"
                value={endTime}
                onChange={(newValue) => {
                  setEndTime(newValue);
                }}
              />
            </LocalizationProvider>
          </Grid>
          <Grid item xs={12}>
            <Box sx={{alignItems: "left"}}>
              <Button variant={"outlined"} type={"submit"}>
                {sessionId ? 'Save changes' : 'Save'}
              </Button>
            </Box>
          </Grid>
        </Grid>
      </form>
    </Stack>
  );
}
