import React, {useEffect, useState} from 'react';
import {
  Autocomplete,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Grid,
  Snackbar,
  TextField
} from "@mui/material";
import {Add, Cancel, Check, Delete, Edit, Save} from "@mui/icons-material";
import {Editing, Editing_, HealthCheckPause, State, State_} from "./healthcheckpauseseditor/model";
import moment from "moment/moment";
import {rootStore} from "../store/rootReducer";
import {
  deleteHealthCheckPauses,
  fetchHealthCheckPauses,
  RequestDeleteHealthCheckPauses,
  RequestSetHealthCheckPauses,
  saveHealthCheckPauses
} from "../store/healthCheckPausesSlice";
import {styled} from "@mui/material/styles";
import {Countries} from "../services/Countries";
import Loading from "./Loading";
import {DatePicker} from "@mui/lab";


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

const StyledDialogContent = styled(DialogContent)(() => ({
  minWidth: 380,
}));

const StyledContentText = styled(DialogContentText)(() => ({
  marginBottom: 10,
  fontSize: 20
}));

const StyledEditableList = styled('div')(() => ({
  marginBottom: 15,
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

export type SetHealthCheckPauses = {
  originalDate: number
  redListUpdate: HealthCheckPause
}

export const HealthCheckEditor = () => {
  const [state, setState] = useState<State>({updates: [], editing: null})
  const [confirm, setConfirm] = useState<Confirm>({kind: 'closed'})
  const [updatesRequested, setUpdatesRequested] = useState<boolean>(false)
  const [updatesReceived, setUpdatesReceived] = useState<boolean>(false)
  const [snackbarMessage, setSnackbarMessage] = useState<string | null>(null)

  const setUpdatesState = (updates: HealthCheckPause[]) => {
    setState({...state, updates: updates})
    setUpdatesReceived(true)
  }

  useEffect(() => {
    if (!updatesRequested) {
      console.log('requesting red list updates')
      setUpdatesRequested(true)
      const d = rootStore.dispatch(fetchHealthCheckPauses(setUpdatesState))
      return () => d.abort()
    }
  }, [updatesRequested, setUpdatesRequested, updatesReceived, setUpdatesReceived])

  const setDate: (e: Date | null) => void = e => {
    (state.editing && e) && setState({...state, editing: Editing_.setEffectiveFrom(state.editing, e.valueOf())})
  }

  const cancelEdit = () => setState({...state, editing: null})

  const saveEdit = (editing: Editing) => {
    const withoutOriginal = state.editing && state.updates.filter(u => u.effectiveFrom !== state.editing?.update.effectiveFrom && u.effectiveFrom !== state.editing?.originalDate)
    const withNew = withoutOriginal && state.editing && withoutOriginal.concat(state.editing.update)
    const request: RequestSetHealthCheckPauses = {
      updates: {
        originalDate: editing.originalDate,
        redListUpdate: editing.update
      },
      onSuccess: () => {
        setSnackbarMessage('Changes saved')
      },
      onFailure: () => {
        setSnackbarMessage('There was a problem saving your changes')
      },
    }
    state.editing && rootStore.dispatch(saveHealthCheckPauses(request))
    withNew && setState({...state, editing: null, updates: withNew})
  }

  function removeAddition(name: string) {
    state.editing && setState({...state, editing: Editing_.removeAddition(state.editing, name)})
  }

  function saveAddition() {
    state.editing && state.editing.addingAddition &&
    setState({
      ...state,
      editing: {
        ...state.editing,
        addingAddition: null,
        update: {
          ...state.editing.update,
          additions: state.editing.update.additions.concat([[state.editing.addingAddition.name, state.editing.addingAddition.code]])
        }
      }
    })
  }

  function cancelAddition() {
    state.editing && setState({...state, editing: {...state.editing, addingAddition: null}})
  }

  function removeRemoval(code: string) {
    state.editing && setState({...state, editing: Editing_.removeRemoval(state.editing, code)})
  }

  function saveRemoval() {
    state.editing && state.editing.addingRemoval &&
    setState({
      ...state,
      editing: {
        ...state.editing,
        addingRemoval: null,
        update: {
          ...state.editing.update,
          removals: state.editing.update.removals.concat(state.editing.addingRemoval)
        }
      }
    })
  }

  function cancelRemoval() {
    state.editing && setState({...state, editing: {...state.editing, addingRemoval: null}})
  }

  function deleteUpdates(effectiveFrom: number) {
    setState({...state, updates: state.updates.filter(u => u.effectiveFrom !== effectiveFrom)})
    const deletionRequest: RequestDeleteHealthCheckPauses = {
      dateToDelete: effectiveFrom,
      onSuccess: () => {
        setSnackbarMessage('Red list updates removed')
      },
      onFailure: () => {
        setSnackbarMessage('There was a problem removing the red list changes')
      }
    }
    rootStore.dispatch(deleteHealthCheckPauses(deletionRequest))
    console.log('Updated state with deleted update. TODO: call end point to persist to all ports')
  }

  const addNewChangeSet = () => {
    setState({
      ...state,
      editing: {
        update: {effectiveFrom: today(), additions: [], removals: []},
        addingAddition: null,
        addingRemoval: null,
        originalDate: today()
      }
    })
  }

  const editChangeSet = (update: HealthCheckPause) => {
    setState({
      ...state,
      editing: {
        update: update,
        addingAddition: null,
        addingRemoval: null,
        originalDate: update.effectiveFrom
      }
    })
  }

  const addNewAddition = () => state.editing && setState(State_.addingAddition(state))

  const addNewRemoval = () => state.editing && setState(State_.addingRemoval(state))

  const today: () => number = () => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  }

  const confirmDeleteChangeSet = (effectiveFrom: number) => () => {
    setConfirm({kind: 'closed'})
    deleteUpdates(effectiveFrom)
  }

  return (
    <RootGrid container={true}>
      <Snackbar
        anchorOrigin={{vertical: 'top', horizontal: 'center'}}
        open={!!snackbarMessage}
        autoHideDuration={6000}
        onClose={() => setSnackbarMessage('')}
        message={snackbarMessage}
      />
      <Grid container={true} item={true}>
        <h1>Red List Changes</h1>
      </Grid>
      <Grid container={true} item={true}>
        <Button color="primary" variant="outlined" size="medium" onClick={addNewChangeSet}>Add a new change set</Button>
      </Grid>
      <TableGrid container={true} item={true}>
        {state.editing &&
        <Dialog open={true} maxWidth="xs">
            <DialogTitle>
                <Grid container={true} alignItems="center">
                    <Grid item={true} xs={4}>Edit changes</Grid>
                    <Grid item={true} xs={8}>
                        <DatePicker renderInput={(params) => <TextField {...params}/>}
                                    value={new Date(state.editing.update.effectiveFrom)}
                                    onChange={setDate}/>
                    </Grid>
                </Grid>
            </DialogTitle>

            <StyledDialogContent>
                <StyledEditableList>
                    <StyledContentText>
                        <Grid container={true}>
                            <Grid item={true} xs={10}>Additions</Grid>
                            <Grid item={true} xs={2}>
                                <Button variant="outlined" size="small" onClick={addNewAddition}>
                                    <Add fontSize="small"/>
                                </Button>
                            </Grid>
                        </Grid>
                    </StyledContentText>
                    <Grid direction="row" container={true}>
                      {state.editing && state.editing.addingAddition &&
                      <Grid item={true} container={true} alignItems="flex-end">
                          <Grid item={true} xs={8}><Autocomplete
                              disablePortal
                              id="combo-box-demo"
                              options={Countries}
                              onChange={(e: React.SyntheticEvent, v: { label: string; code: string } | null) =>
                                v && setState(State_.updatingAddition(state, v.label, v.code))
                              }
                              sx={{width: 225}}
                              renderInput={(params) => <TextField {...params} label="Country" variant='standard'/>}
                          /></Grid>
                          <Grid item={true} xs={2}>
                              <Button color="primary" variant="outlined" size="small" onClick={saveAddition}><Check
                                  fontSize="small"/></Button>
                          </Grid>
                          <Grid item={true} xs={2}>
                              <Button color="primary" variant="outlined" size="small" onClick={cancelAddition}><Delete
                                  fontSize="small"/></Button>
                          </Grid>
                      </Grid>
                      }
                      {state.editing.update.additions.map(nameCode => {
                        return <Grid item={true} container={true} alignItems="center">
                          <Grid item={true} xs={10}>{nameCode[0]}</Grid>
                          <Grid item={true} xs={2}>
                            <Button color="primary" variant="outlined" size="small"
                                    onClick={() => removeAddition(nameCode[0])}>
                              <Delete fontSize="small"/>
                            </Button>
                          </Grid>
                        </Grid>
                      })}
                    </Grid>
                </StyledEditableList>
                <StyledEditableList>
                    <StyledContentText>
                        <Grid direction="row" container={true}>
                            <Grid item={true} xs={10}>Removals</Grid>
                            <Grid item={true} xs={2}>
                                <Button color="primary" variant="outlined" size="small" onClick={addNewRemoval}>
                                    <Add fontSize="small"/>
                                </Button>
                            </Grid>
                        </Grid>
                    </StyledContentText>
                    <Grid>
                      {state.editing && state.editing.addingRemoval !== null &&
                      <Grid item={true} container={true} alignItems="flex-end">
                          <Grid item={true} xs={8}><Autocomplete
                              disablePortal
                              id="combo-box-demo"
                              options={Countries}
                              onChange={(e: React.SyntheticEvent, v: { label: string; code: string } | null) =>
                                v && setState(State_.updatingRemoval(state, v.label))
                              }
                              sx={{width: 225}}
                              renderInput={(params) => <TextField {...params} label="Country" variant='standard'/>}
                          /></Grid>
                          <Grid item={true} xs={2}><Button color="primary" variant="outlined" size="small"
                                                           onClick={saveRemoval}><Check
                              fontSize="small"/></Button></Grid>
                          <Grid item={true} xs={2}><Button color="primary" variant="outlined" size="small"
                                                           onClick={cancelRemoval}><Delete
                              fontSize="small"/></Button></Grid>
                      </Grid>
                      }
                      {state.editing.update.removals.map(removalCode => {
                        return <Grid item={true} container={true}>
                          <Grid item={true} xs={10}>{removalCode}</Grid>
                          <Grid item={true} xs={2}>
                            <Button color="primary" variant="outlined" size="small"
                                    onClick={() => removeRemoval(removalCode)}>
                              <Delete fontSize="small"/>
                            </Button>
                          </Grid>
                        </Grid>
                      })}
                    </Grid>
                </StyledEditableList>
            </StyledDialogContent>
            <DialogActions>
                <Button color="primary" variant="outlined" size="medium" onClick={() => cancelEdit()}>
                    <Cancel/> Cancel
                </Button>
                <Button color="primary" variant="outlined" size="medium"
                        onClick={() => state.editing && saveEdit(state.editing)}>
                    <Save/> Save
                </Button>
            </DialogActions>
        </Dialog>}
        {confirm.kind === 'open' &&
        <Dialog open={true} maxWidth="xs">
            <DialogTitle>{confirm.message}</DialogTitle>
            <DialogActions>
                <Button color="primary" variant="outlined" size="medium" onClick={() => setConfirm({kind: 'closed'})}
                        key="no">
                    No
                </Button>
                <Button color="primary" variant="outlined" size="medium" onClick={confirm.onConfirm} key="yes">
                    Yes
                </Button>
            </DialogActions>
        </Dialog>}
        {updatesReceived ? <Grid container={true}>
          <React.Fragment>
            <Grid container={true} item={true} xs={8}>
              <TitleGridItem item={true} xs={4}>Effective from</TitleGridItem>
              <TitleGridItem item={true} xs={4}>Additions</TitleGridItem>
              <TitleGridItem item={true} xs={4}>Removals</TitleGridItem>
            </Grid>
            <TitleGridItem item={true} xs={4}/>
          </React.Fragment>
          {state.updates.sort((a, b) => -1 * (a.effectiveFrom - b.effectiveFrom)).map(update => {
            return <React.Fragment key={update.effectiveFrom}>
              <Grid container={true} item={true} xs={8}>
                <RowGridItem item={true} xs={4}>{moment(update.effectiveFrom).format("Do MMM YYYY")}</RowGridItem>
                <RowGridItem item={true} xs={4}>{update.additions.length}</RowGridItem>
                <RowGridItem item={true} xs={4}>{update.removals.length}</RowGridItem>
              </Grid>
              <RowGridItem item={true} xs={2}><Button color="primary" variant="outlined"
                                                        size="medium"
                                                        onClick={() => editChangeSet(update)}><Edit/></Button></RowGridItem>
              <RowGridItem item={true} xs={2}><Button color="primary" variant="outlined"
                                                        size="medium"
                                                        onClick={() => setConfirm({
                                                          kind: 'open',
                                                          message: 'Are you sure you want to remove this set of changes?',
                                                          onConfirm: confirmDeleteChangeSet(update.effectiveFrom)
                                                        })}><Delete/></Button></RowGridItem>
            </React.Fragment>
          })
          }
        </Grid> : <Loading/>}
      </TableGrid>
    </RootGrid>
  );
}
