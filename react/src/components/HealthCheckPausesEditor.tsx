import React, {useState} from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Snackbar,
  Stack,
  TextField
} from "@mui/material";
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

  const columns = [
    {
      field: 'startsAt',
      headerName: 'From',
      valueGetter: (params: any) => moment(params.row.startsAt).format("HH:mm, Do MMM YYYY"),
      width: 200
    },
    {
      field: 'endsAt',
      headerName: 'To',
      valueGetter: (params: any) => moment(params.row.endsAt).format("HH:mm, Do MMM YYYY"),
      width: 200
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


  const today: () => Moment = () => {
    return moment()
  }

  function addNewPause() {
    setNewPause({
      startsAt: today().valueOf(),
      endsAt: today().valueOf(),
      ports: [],
      createdAt: today().valueOf(),
    })
  }

  const setStart: (e: Moment | null) => void = e => {
    (newPause && e) && setNewPause({...newPause, startsAt: e.valueOf()})
  }

  const setEnd: (e: Moment | null) => void = e => {
    (newPause && e) && setNewPause({...newPause, endsAt: e.valueOf()})
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

  const rows = healthCheckPauses.sort((a, b) => -1 * (a.startsAt.valueOf() - b.startsAt.valueOf()))

  return (
    <Stack sx={{my: 2, gap: 2}}>
      <LocalizationProvider dateAdapter={AdapterMoment}>
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
                          <DateTimePicker renderInput={(params) => <TextField {...params}/>}
                                          value={moment(newPause.startsAt)}
                                          label={'From'}
                                          onChange={setStart}/>
                          <DateTimePicker renderInput={(params) => <TextField {...params}/>}
                                          value={moment(newPause.endsAt)}
                                          label={'To'}
                                          onChange={setEnd}/>
                      </Stack>
                  </DialogContent>
                  <DialogActions>
                      <Button color="primary" variant="outlined" size="medium" onClick={() => setNewPause(null)}>
                          <Cancel/> Cancel
                      </Button>
                      <Button color="primary" variant="outlined" size="medium"
                              onClick={() => newPause && saveNewPause(newPause)}>
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
                <Typography variant={'body1'}>There are no existing pauses</Typography> :
                <Box sx={{height: 400, width: '100%'}}>
                  <DataGrid
                    rows={rows}
                    columns={columns}
                    getRowId={(r) => r.createdAt.valueOf().toString()}
                    disableSelectionOnClick={true}
                  />
                </Box>
          }
        </Box>
      </LocalizationProvider>
    </Stack>
  );
}
