import React, {useState} from 'react';
import {
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
import {styled} from "@mui/material/styles";
import Loading from "./Loading";
import {deleteHealthCheckPause, saveHealthCheckPauses, useHealthCheckPauses} from "../store/heathCheckPausesSlice";
import Typography from "@mui/material/Typography";
import {DateTimePicker} from "@mui/x-date-pickers/DateTimePicker";
import {AdapterMoment} from "@mui/x-date-pickers/AdapterMoment";
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider';
import {Moment} from "moment";
import {rootStore} from "../store/rootReducer";


const RootGrid = styled(Grid)(() => ({
  flexGrow: 1,
  maxWidth: 800,
}));

const TableGrid = styled(Grid)(() => ({
  marginTop: 25
}));

const TitleGridItem = styled(Grid)(({theme}) => ({
  padding: theme.spacing(1),
  color: theme.palette.text.secondary,
  fontSize: 20,
}));

const RowGridItem = styled(Grid)(({theme}) => ({
  padding: theme.spacing(1),
  color: theme.palette.text.primary,
}));

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
    rootStore.dispatch(saveHealthCheckPauses({
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
    }))
  }

  const deletePause = (from: number, to: number) => {
    console.log(`Deleting pause ${from} to ${to}`)
    rootStore.dispatch(deleteHealthCheckPause({
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
    }))
  }

  const confirmDeletePause = (from: number, to: number) => () => {
    setConfirm({kind: 'closed'})
    deletePause(from, to)
  }

  return (
    <RootGrid container={true}>
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
        <TableGrid container={true} item={true}>
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
                <Grid container={true}>
                  <React.Fragment>
                    <Grid container={true} item={true} xs={8}>
                      <TitleGridItem item={true} xs={4}>From</TitleGridItem>
                      <TitleGridItem item={true} xs={4}>To</TitleGridItem>
                    </Grid>
                    <TitleGridItem item={true} xs={4}/>
                  </React.Fragment>
                  {healthCheckPauses.sort((a, b) => -1 * (a.startsAt.valueOf() - b.startsAt.valueOf())).map(pause => {
                    return <React.Fragment key={pause.startsAt.valueOf()}>
                      <Grid container={true} item={true} xs={8}>
                        <RowGridItem item={true}
                                     xs={4}>{moment(pause.startsAt).format("HH:MM, Do MMM YYYY")}</RowGridItem>
                        <RowGridItem item={true}
                                     xs={4}>{moment(pause.endsAt).format("HH:MM, Do MMM YYYY")}</RowGridItem>
                      </Grid>
                      <RowGridItem item={true} xs={2}><Button color="primary" variant="outlined"
                                                              size="medium"
                                                              onClick={() => setConfirm({
                                                                kind: 'open',
                                                                message: 'Are you sure you want to remove this health check pause?',
                                                                onConfirm: confirmDeletePause(pause.startsAt, pause.endsAt)
                                                              })}><Delete/></Button></RowGridItem>
                    </React.Fragment>
                  })
                  }
                </Grid>
          }
        </TableGrid>
      </LocalizationProvider>
    </RootGrid>
  );
}
