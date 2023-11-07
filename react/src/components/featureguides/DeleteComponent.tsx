import {Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";
import Button from "@mui/material/Button";
import React, {useState} from "react";
import axios, {AxiosResponse} from "axios";

interface Props {
  id: string | undefined;
  showDelete: boolean;
  setShowDelete: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  setOpenPreview: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  setReceivedData: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export function DeleteComponent(props: Props) {
  const [error, setError] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const handleResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      setConfirmDelete(true);
      console.log('Deleted Feature Guide');
    } else {
      setError(true);
      response.data
    }
  }

  const handleErrorDialogClose = () => {
    setError(false);
    props.setShowDelete(false);
  }

  const handleCloseConfirmDialog = () => {
    setConfirmDelete(false);
    props.setShowDelete(false);
    props.setOpenPreview(false);
    props.setReceivedData(false);
  }

  const handleConfirmDialog = () => {
    confirmFeatureDelete();
  }

  const confirmFeatureDelete = () => {
    axios.delete('/guide/removeFeatureGuide/' + props.id)
      .then(response => handleResponse(response))
  }
  return (
    error ?
      <Dialog open={error} onClose={handleErrorDialogClose}>
        <DialogTitle>Error</DialogTitle>
        <DialogContent>
          <p>Error while deleting</p>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleErrorDialogClose}>Close</Button>
        </DialogActions>
      </Dialog> :
      confirmDelete ?
        <Dialog open={confirmDelete} onClose={handleCloseConfirmDialog}>
          <DialogTitle>Feature deleted</DialogTitle>
          <DialogContent>
            <p>Feature requested is deleted</p>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseConfirmDialog}>Close</Button>
          </DialogActions>
        </Dialog> :
        <Dialog open={props.showDelete} onClose={handleCloseConfirmDialog}>
          <DialogTitle>Confirm Delete</DialogTitle>
          <DialogContent>
            <p>Are you sure you want to delete this item?</p>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseConfirmDialog}>Cancel</Button>
            <Button onClick={handleConfirmDialog} variant="contained" color="error">
              Delete
            </Button>
          </DialogActions>
        </Dialog>
  )
}
