import React, {useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, DialogTitle, Grid, Snackbar, Stack} from "@mui/material";
import {Cancel, Delete, Save} from "@mui/icons-material";
import {ScheduledHealthCheckPause} from "./healthcheckpauseseditor/model";
import moment from "moment-timezone";
import Loading from "./Loading";
import {deleteHealthCheckPause, saveHealthCheckPauses, useHealthCheckPauses} from "../store/heathCheckPausesSlice";
import Typography from "@mui/material/Typography";
import {DateTimePicker} from "@mui/x-date-pickers/DateTimePicker";
import {AdapterMoment} from "@mui/x-date-pickers/AdapterMoment";
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider';
import {Moment} from "moment";
import {DataGrid} from "@mui/x-data-grid";
import {Alert} from "@mui/lab";

type ConfirmOpen = {
  kind: 'open'
  message: string
  onConfirm: () => void
}
type ConfirmClosed = {
  kind: 'closed'
}

type Confirm = ConfirmOpen | ConfirmClosed


export const HealthCheckEditor = () => {
  const [requestRefreshAt, setRequestRefreshAt] = useState<Moment>(moment())
  const {healthCheckPauses, loading, failed} = useHealthCheckPauses(requestRefreshAt.valueOf())
  const [newPause, setNewPause] = useState<ScheduledHealthCheckPause | null>(null)
  const [confirm, setConfirm] = useState<Confirm>({kind: 'closed'})
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null)
  const [newPauseIsValid, setNewPauseIsValid] = useState<boolean>(true)

  moment.locale('en-gb')

  const columns = [
    {
      field: 'pause',
      headerName: 'Pause',
      valueGetter: (params: any) => {
        const sameStartDate = moment(params.row.startsAt).isSame(moment(params.row.endsAt), 'day')
        return sameStartDate ?
          `${moment(params.row.startsAt).format("HH:mm")} to ${moment(params.row.endsAt).format("HH:mm, Do MMM YYYY")}` :
          `${moment(params.row.startsAt).format("HH:mm, Do MMM YYYY")} to ${moment(params.row.endsAt).format("HH:mm, Do MMM YYYY")}`
      },
      width: 400
    },
    {
      field: 'createdAt',
      headerName: 'Created',
      valueGetter: (params: any) => moment(params.row.createdAt).format("HH:mm, Do MMM YYYY"),
      width: 200
    },
    {
      field: 'delete',
      headerName: '',
      width: 200,
      renderCell: (params: any) =>
        <Button color="primary" variant="outlined"
                size="medium"
                onClick={() => setConfirm({
                  kind: 'open',
                  message: 'Are you sure you want to remove this health check pause?',
                  onConfirm: confirmDeletePause(params.row.startsAt, params.row.endsAt),
                })}><Delete/></Button>
    },
  ]


  const today: () => Moment = () => moment()

  function addNewPause() {
    const now = today()
    const monthOfTheYear = now.month() + 1
    const str = `${now.year()}-${monthOfTheYear.toString().padStart(2, '0')}-${now.date().toString().padStart(2, '0')}T${now.hour() + 1}:00:00.000Z`
    const nextHour = moment(str)
    console.log(`str: ${str}, nextHour: ${nextHour.format()}`)
    setNewPause({
      startsAt: nextHour.valueOf(),
      endsAt: nextHour.add(1, 'hour').valueOf(),
      ports: [],
      createdAt: now.valueOf(),
    })
  }

  const setStart: (e: Moment | null) => void = e => {
    if (newPause && e) {
      const pause = {...newPause, startsAt: e.valueOf()}
      setNewPause(pause)
      validate(pause)
    }
  }

  const setEnd: (e: Moment | null) => void = e => {
    if (newPause && e) {
      const pause = {...newPause, endsAt: e.valueOf()}
      setNewPause(pause)
      validate(pause)
    }
  }

  const validate = (pause: ScheduledHealthCheckPause) => {
    setNewPauseIsValid(pause.startsAt < pause.endsAt)
  }

  const saveNewPause = (pause: ScheduledHealthCheckPause) => {
    console.log(`Saving new pause ${JSON.stringify(pause)}`)
    saveHealthCheckPauses({
      pause: pause,
      onSuccess: () => {
        console.log(`Saved new pause ${JSON.stringify(pause)}`)
        setNewPause(null)
        setRequestRefreshAt(moment())
      },
      onFailure: () => {
        console.log(`Failed to save new pause ${JSON.stringify(pause)}`)
        setSnackbarMessage('Sorry, I couldn\'t save the new pause. Please try again later.')
      }
    })
  }

  const deletePause = (from: number, to: number) => {
    console.log(`Deleting pause ${from} to ${to}`)
    deleteHealthCheckPause({
      startsAt: from,
      endsAt: to,
      onSuccess: () => {
        console.log(`Deleted pause ${from} to ${to}`)
        setRequestRefreshAt(moment())
      },
      onFailure: () => {
        console.log(`Failed to delete pause ${from} to ${to}`)
        setSnackbarMessage('Sorry, I couldn\'t delete the pause. Please try again later.')
      }
    })
  }

  const confirmDeletePause = (from: number, to: number) => () => {
    setConfirm({kind: 'closed'})
    deletePause(from, to)
  }

  const rows = [...healthCheckPauses]
    .sort((a, b) => -1 * (b.startsAt.valueOf() - a.startsAt.valueOf()))

  return (
    <Stack sx={{my: 2, gap: 2}}>
      <LocalizationProvider dateAdapter={AdapterMoment} adapterLocale={'en-gb'}>
        <Snackbar
          anchorOrigin={{vertical: 'top', horizontal: 'center'}}
          open={!!snackbarMessage}
          autoHideDuration={6000}
          onClose={() => setSnackbarMessage('')}
          message={snackbarMessage}
        />
        <Grid container={true} item={true}>
          <h1>Scheduled health check pauses</h1>
        </Grid>
        <Grid container={true} item={true}>
          <Button color="primary" variant="outlined" size="medium" onClick={addNewPause}>Add a new pause</Button>
        </Grid>
        <Box>
          {newPause &&
              <Dialog open={true} maxWidth="xs">
                  <DialogTitle>
                      Add a pause
                  </DialogTitle>
                  <DialogContent sx={{minWidth: 380}}>
                      <Stack sx={{my: 2, gap: 2}}>
                        {!newPauseIsValid && <Alert severity="error">The start time must be before the end time</Alert>}
                          <DateTimePicker
                              slotProps={{textField: {variant: 'outlined'}}}
                              value={moment(newPause.startsAt)}
                              label={'From'}
                              onChange={setStart}/>
                          <DateTimePicker
                              slotProps={{textField: {variant: 'outlined'}}}
                              value={moment(newPause.endsAt)}
                              label={'To'}
                              onChange={setEnd}
                          />
                      </Stack>
                  </DialogContent>
                  <DialogActions>
                      <Button color="primary" variant="outlined" size="medium"
                              onClick={() => setNewPause(null)}>
                          <Cancel/> Cancel
                      </Button>
                      <Button color="primary" variant="outlined" size="medium"
                              onClick={() => newPause && saveNewPause(newPause)}
                              disabled={!newPauseIsValid}>
                          <Save/> Save
                      </Button>
                  </DialogActions>
              </Dialog>}
          {confirm.kind === 'open' &&
              <Dialog open={true} maxWidth="xs">
                  <DialogTitle>{confirm.message}</DialogTitle>
                  <DialogActions>
                      <Button color="primary" variant="outlined" size="medium"
                              onClick={() => setConfirm({kind: 'closed'})}
                              key="no">
                          No
                      </Button>
                      <Button color="primary" variant="outlined" size="medium" onClick={confirm.onConfirm} key="yes">
                          Yes
                      </Button>
                  </DialogActions>
              </Dialog>}
          {loading ?
            <Loading/> :
            failed ?
              <Typography variant={'body1'}>Sorry, I couldn't load the existing pauses</Typography> :
              healthCheckPauses.length === 0 ?
                <Typography variant={'body1'}>There are no current or upcoming pauses</Typography> :
                <Box sx={{height: 400, width: '100%'}}>
                  <DataGrid
                    rows={rows}
                    columns={columns}
                    getRowId={(r) => r.createdAt.valueOf().toString()}
                    disableRowSelectionOnClick={true}
                  />
                </Box>
          }
        </Box>
      </LocalizationProvider>
    </Stack>
  );
}
