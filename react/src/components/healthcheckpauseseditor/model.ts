export type HealthCheckPause = {
  from: number
  to: number
}

export type HealthCheckPauses = {
  updates: HealthCheckPause[]
}

export type HealthCheckPausesLoaded = {
  kind: "HealthCheckPausesLoaded"
  updates: HealthCheckPauses
}

export type HealthCheckPausesPending = {
  kind: "HealthCheckPausesPending"
}

export type HealthCheckPausesState = HealthCheckPausesPending | HealthCheckPausesLoaded

export type Editing = {
  update: HealthCheckPause
}

export class Editing_ {

  public static setAddition(editing: Editing, country: string, code: string): Editing {
    const updatedAddition = editing.addingAddition && {...editing.addingAddition, name: country, code: code}
    return {...editing, addingAddition: updatedAddition}
  }

  public static setRemovalCode(editing: Editing, code: string): Editing {
    return {...editing, addingRemoval: code}
  }

  public static setEffectiveFrom(editing: Editing, newDate: number): Editing {
    return {...editing, update: {...editing.update, effectiveFrom: newDate}}
  }

  public static removeAddition(editing: Editing, name: string): Editing {
    const additions = editing.update.additions.filter(entry => entry[0] !== name)
    return {...editing, update: {...editing.update, additions: additions}}
  }

  public static removeRemoval(editing: Editing, code: string): Editing {
    const removals = editing.update.removals.filter(c => c !== code)
    return {...editing, update: {...editing.update, removals: removals}}
  }
}

export type State = {
  updates: HealthCheckPause[],
  editing: Editing | null
}

export class State_ {
  public static addingAddition(state: State): State {
    return state.editing ? {...state, editing: {...state.editing, addingAddition: {name: "", code: ""}}} : state
  }

  public static updatingAddition(state: State, country: string, code: string): State {
    return state.editing ? {...state, editing: Editing_.setAddition(state.editing, country, code)} : state
  }

  public static addingRemoval(state: State): State {
    return state.editing ? {...state, editing: {...state.editing, addingRemoval: ""}} : state
  }

  public static updatingRemoval(state: State, country: string): State {
    return state.editing ? {...state, editing: Editing_.setRemovalCode(state.editing, country)} : state
  }
}
