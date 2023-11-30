import React from "react";
import {Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";
import Button from "@mui/material/Button";

import MuiAlert, {AlertProps} from '@mui/material/Alert';

export const Alert = React.forwardRef<HTMLDivElement, AlertProps>(function Alert(
  props,
  ref,
) {
  return <MuiAlert elevation={6} ref={ref} variant="filled" {...props} />;
});


interface Props {
  actionText: string
  onConfirm: () => void
  onCancel: () => void
}

export function DialogComponent(props: Props) {
  return <div>
    <Dialog open={true} onClose={props.onCancel}>
      <DialogTitle>Confirm {props.actionText}</DialogTitle>
      <DialogContent>
        <p>Are you sure you want to {props.actionText} this item?</p>
      </DialogContent>
      <DialogActions>
        <Button onClick={props.onCancel}>Cancel</Button>
        <Button onClick={props.onConfirm} variant="contained" color="error">
          {props.actionText}
        </Button>
      </DialogActions>
    </Dialog>
  </div>
}
