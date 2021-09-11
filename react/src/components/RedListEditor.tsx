import React, {useState} from 'react';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Grid,
  TextField
} from "@material-ui/core";
import {DatePicker} from "@material-ui/pickers";
import {MaterialUiPickersDate} from "@material-ui/pickers/typings/date";
import {Add, Cancel, Check, Delete, Edit, Save} from "@material-ui/icons";


export type RedListUpdate = {
  effectiveFrom: number
  additions: [string, string][]
  removals: string[]
}

export type RedListUpdates = {
  updates: Map<number, RedListUpdate>
}

type Addition = {
  name: string
  code: string
}

type Editing = {
  update: RedListUpdate
  addingAddition: Addition | null
  addingRemoval: string | null
  originalDate: number
}

class Editing_ {
  public static setAdditionName(editing: Editing, name: string): Editing {
    const updatedAddition = editing.addingAddition && {...editing.addingAddition, name: name}
    return {...editing, addingAddition: updatedAddition}
  }

  public static setAdditionCode(editing: Editing, code: string): Editing {
    const updatedAddition = editing.addingAddition && {...editing.addingAddition, code: code}
    return {...editing, addingAddition: updatedAddition}
  }

  public static setRemovalCode(editing: Editing, code: string): Editing {
    return {...editing, addingRemoval: code}
  }

  public static setEffectiveFrom(editing: Editing, newDate: number): Editing {
    return {...editing, update: {...editing.update, effectiveFrom: newDate}}
  }

  public static removeAddition(editing: Editing, name: string): Editing {
    const additions = editing.update.additions.filter(entry => entry[0] != name)
    return {...editing, update: {...editing.update, additions: additions}}
  }

  public static removeRemoval(editing: Editing, code: string): Editing {
    const removals = editing.update.removals.filter(c => c != code)
    return {...editing, update: {...editing.update, removals: removals}}
  }
}

type State = {
  updates: RedListUpdate[],
  editing: Editing | null
}

class State_ {
  public static addingAddition(state: State): State {
    return state.editing ? {...state, editing: {...state.editing, addingAddition: {name: "", code: ""}}} : state
  }

  public static updatingAdditionName(state: State, event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>): State {
    return state.editing ? {...state, editing: Editing_.setAdditionName(state.editing, event.target.value)} : state
  }

  public static updatingAdditionCode(state: State, event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>): State {
    return state.editing ? {...state, editing: Editing_.setAdditionCode(state.editing, event.target.value)} : state
  }

  public static addingRemoval(state: State): State {
    return state.editing ? {...state, editing: {...state.editing, addingRemoval: ""}} : state
  }

  public static updatingRemoval(state: State, event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>): State {
    return state.editing ? {...state, editing: Editing_.setRemovalCode(state.editing, event.target.value)} : state
  }
}


export const RedListEditor = () => {
  const [state, setState] = useState<State>({updates: [], editing: null})

  const setDate: (e: MaterialUiPickersDate) => void = e => {
    setState((state.editing && e) ? {
      ...state,
      editing: Editing_.setEffectiveFrom(state.editing, e.milliseconds())
    } : state)
  }

  const cancelEdit = () => setState({...state, editing: null})

  const saveEdit = () => {
    const withoutOriginal = state.editing && state.updates.filter(u => u.effectiveFrom != state.editing?.update.effectiveFrom && u.effectiveFrom != state.editing?.originalDate)
    const withNew = withoutOriginal && state.editing && withoutOriginal.concat(state.editing.update)
    console.log('Updated state with saved edit. TODO: call endpoints to persist to all ports')
    withNew && setState({...state, editing: null, updates: withNew})
  }

  function removeAddition(name: string) {
    state.editing && setState({...state, editing: Editing_.removeAddition(state.editing, name)})
  }

  function saveAddition() {
    state.editing && state.editing.addingAddition && console.log('concat result: ' + state.editing.update.additions.concat([[state.editing.addingAddition.name, state.editing.addingAddition.code]]))
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
    setState({...state, updates: state.updates.filter(u => u.effectiveFrom != effectiveFrom)})
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

  const editChangeSet = (update: RedListUpdate) => {
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

  return <div>
    <h2>Red List Change Sets</h2>
    <Button color="primary" variant="outlined" size="medium" onClick={addNewChangeSet}>Add a new change set</Button>
    {state.editing &&
    <Dialog open={true} maxWidth="xs">
        <DialogTitle>Edit changes for {new Date(state.editing.update.effectiveFrom).toISOString()}</DialogTitle>
        <DialogContent>
            <DatePicker value={state.editing.update.effectiveFrom} onChange={setDate}/>
            <DialogContentText>
                Additions
                <Button color="default" variant="outlined" size="small" onClick={addNewAddition}>
                    <Add fontSize="small"/>
                </Button>
            </DialogContentText>
            <Grid direction="row" container={true}>
              {state.editing && state.editing.addingAddition &&
              <Grid item={true} container={true}>
                  <Grid item={true} xs={4}><TextField label="Full name" value={state.editing.addingAddition.name}
                                                      onChange={e => setState(State_.updatingAdditionName(state, e))}/></Grid>
                  <Grid item={true} xs={4}><TextField label="3 letter code" value={state.editing.addingAddition.code}
                                                      onChange={e => setState(State_.updatingAdditionCode(state, e))}/></Grid>
                  <Grid item={true} xs={2}><Button color="default" variant="outlined" size="small"
                                                   onClick={saveAddition}><Check fontSize="small"/></Button></Grid>
                  <Grid item={true} xs={2}><Button color="default" variant="outlined" size="small"
                                                   onClick={cancelAddition}><Delete fontSize="small"/></Button></Grid>
              </Grid>
              }
              {state.editing.update.additions.map(nameCode => {
                console.log('nameCode: ' + nameCode)
                return <Grid item={true} container={true}>
                  <Grid item={true} xs={10}>{nameCode[0]} ({nameCode[1]})</Grid>
                  <Grid item={true} xs={2}>
                    <Button color="default" variant="outlined" size="small">
                      <Delete fontSize="small"/>
                    </Button>
                  </Grid>
                </Grid>
              })}
            </Grid>
            <DialogContentText>
                Removals
                <Button color="default" variant="outlined" size="small" onClick={addNewRemoval}>
                    <Add fontSize="small"/>
                </Button>
            </DialogContentText>
            <Grid>
              {state.editing && state.editing.addingRemoval !== null &&
              <Grid item={true} container={true}>
                  <Grid item={true} xs={8}><TextField label="Full name" value={state.editing.addingRemoval}
                                                      onChange={e => setState(State_.updatingRemoval(state, e))}/></Grid>
                  <Grid item={true} xs={2}><Button color="default" variant="outlined" size="small"
                                                   onClick={saveRemoval}><Check fontSize="small"/></Button></Grid>
                  <Grid item={true} xs={2}><Button color="default" variant="outlined" size="small"
                                                   onClick={cancelRemoval}><Delete fontSize="small"/></Button></Grid>
              </Grid>
              }
              {state.editing.update.removals.map(removalCode => {
                return <Grid item={true} container={true}>
                  <Grid item={true} xs={10}>{removalCode}</Grid>
                  <Grid item={true} xs={2}>
                    <Button color="default" variant="outlined" size="small">
                      <Delete fontSize="small"/>
                    </Button>
                  </Grid>
                </Grid>
              })}
            </Grid>
        </DialogContent>
        <DialogActions>
            <Button color="default" variant="outlined" size="medium" onClick={() => cancelEdit()}>
                <Cancel/> Cancel
            </Button>
            <Button color="default" variant="outlined" size="medium" onClick={() => saveEdit()}>
                <Save/> Save
            </Button>
        </DialogActions>
    </Dialog>}
    <Grid container={true}>
      {state.updates.map(update => {
        return <Grid container={true} item={true}>
          <Grid item={true}>{update.effectiveFrom}</Grid>
          <Grid item={true}>{update.additions.length} additions</Grid>
          <Grid item={true}>{update.removals.length} removals</Grid>
          <Grid item={true}><Button color="default" variant="outlined" size="medium"
                                    onClick={() => editChangeSet(update)}><Edit/></Button></Grid>
          <Grid item={true}><Button color="default" variant="outlined" size="medium"
                                    onClick={() => deleteUpdates(update.effectiveFrom)}><Delete/></Button></Grid>
        </Grid>
      })
      }
    </Grid>
  </div>
}
