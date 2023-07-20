import {Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";
import Button from "@mui/material/Button";
import React, {useState} from "react";
import axios, {AxiosResponse} from "axios";

interface Props {
  id: string | undefined;
  actionString: string | undefined;
  showAction: boolean;
  setShowAction: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  setReceivedData: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export function PublishComponent(props: Props) {
  const [error, setError] = useState(false);
  const [confirmAction, setConfirmAction] = useState(false);
  const handleResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      console.log('Feature Guide' + props.actionString);
    } else {
      setError(true);
      response.data
    }
  }

  const handleErrorDialogClose = () => {
    setError(false);
    props.setShowAction(false);
  }

  const handleCloseConfirmDialog = () => {
    setConfirmAction(false);
    props.setShowAction(false);
    props.setReceivedData(false);
  }

  const updatePublishState = () => {
    confirmPublishFeature();
  }

  const confirmPublishFeature = () => {
    axios.post('/guide/published/' + props.id, {published: props.actionString == "publish"})
      .then(response => handleResponse(response))
      .then(data => {
        setConfirmAction(true);
        console.log(data);
      }).catch(error => {
      setError(true);
      console.error(error);
    })
  }
  return (
    error ?
      <Dialog open={error} onClose={handleErrorDialogClose}>
        <DialogTitle>Error</DialogTitle>
        <DialogContent>
          <p>Error while {props.actionString == "publish" ? "Publish" : "UnPublish"}</p>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleErrorDialogClose}>Close</Button>
        </DialogActions>
      </Dialog> :
      confirmAction ?
        <Dialog open={confirmAction} onClose={handleCloseConfirmDialog}>
          <DialogTitle>Feature Published</DialogTitle>
          <DialogContent>
            <p>Feature requested is {props.actionString == "publish" ? "Publish" : "UnPublish"}</p>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseConfirmDialog}>Close</Button>
          </DialogActions>
        </Dialog> :
        <Dialog open={props.showAction} onClose={handleCloseConfirmDialog}>
          <DialogTitle>Confirm {props.actionString == "publish" ? "Publish" : "UnPublish"}</DialogTitle>
          <DialogContent>
            <p>Are you sure you want
              to {props.actionString == "publish" ? "Publish" : "UnPublish"} this item?</p>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseConfirmDialog}>Cancel</Button>
            <Button onClick={updatePublishState} variant="contained" color="error">
              {props.actionString == "publish" ? "Publish" : "UnPublish"}
            </Button>
          </DialogActions>
        </Dialog>
  )
}
