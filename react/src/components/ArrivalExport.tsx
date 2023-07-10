import * as React from 'react';
import Button from '@mui/material/Button';
import ExportDatePicker from "./ExportDatePicker";
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import {Dialog, DialogContent, DialogContentText, DialogTitle, IconButton} from "@mui/material";
import {styled} from "@mui/material/styles";
import CloseIcon from '@mui/icons-material/Close';

interface IProps {
  region: string;
}

const BootstrapDialog = styled(Dialog)(({ theme }) => ({
  '& .MuiDialogContent-root': {
    padding: theme.spacing(2),
  },
  '& .MuiDialogActions-root': {
    padding: theme.spacing(1),
  },
}));

export interface DialogTitleProps {
  id: string;
  children?: React.ReactNode;
  onClose: () => void;
}

function BootstrapDialogTitle(props: DialogTitleProps) {
  const { children, onClose } = props;

  return (
    <DialogTitle sx={{ m: 0, p: 2 }}>
      {children}
      {onClose ? (
        <IconButton
          aria-label="close"
          onClick={onClose}
          sx={{
            position: 'absolute',
            right: 8,
            top: 8,
            color: (theme) => theme.palette.grey[500],
          }}
        >
          <CloseIcon />
        </IconButton>
      ) : null}
    </DialogTitle>
  );
}

export default function ArrivalExport(props: IProps) {
  const [open, setOpen] = React.useState(false);
  const handleOpen = () => setOpen(true);
  const handleClose = () => setOpen(false);

  return (
    <div>
      <Button startIcon={<FileDownloadIcon/>} onClick={handleOpen}>
        {props.region} region Export
      </Button>
      <BootstrapDialog
        open={open}
        onClose={handleClose}
        aria-labelledby="modal-modal-title"
        aria-describedby="modal-modal-description">
        <BootstrapDialogTitle id="customised-dialog-title" onClose={handleClose}>{props.region} region arrivals</BootstrapDialogTitle>
        <DialogContent>
          <DialogContentText id="modal-modal-description">
            Choose dates and download arrivals.
          </DialogContentText>
          <ExportDatePicker region={props.region} handleClose={handleClose}/>
        </DialogContent>
      </BootstrapDialog>
    </div>
  );
}
