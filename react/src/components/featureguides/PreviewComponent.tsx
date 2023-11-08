import React, {useState} from "react";
import {Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import {DeleteComponent} from "./DeleteComponent";

interface Props {
  id?: string;
  videoURL: string;
  title: string | undefined;
  markdownContent: string | undefined;
  openPreview: boolean;
  setOpenPreview?: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  setReceivedData?: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  showEdit?: boolean;
  setShowEdit?: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  isEdit?: boolean;
}

export function PreviewComponent(props: Props) {
  const [showDelete, setShowDelete] = useState(false);

  const handleEdit = () => {
    props.setShowEdit && props.setShowEdit(true);
    props.setOpenPreview && props.setOpenPreview(false)
  }

  const handlePreviewClose = () => {
    props.setOpenPreview && props.setOpenPreview(false)
  }

  const handleDelete = () => {
    setShowDelete(true);
  }

  return (
    (showDelete && props.setOpenPreview && props.setReceivedData) ?
      <DeleteComponent id={props.id} showDelete={showDelete} setShowDelete={setShowDelete}
                       setOpenPreview={props.setOpenPreview} setReceivedData={props.setReceivedData}/> :
      <div>
        <Dialog open={props.openPreview} maxWidth="lg" onClose={handlePreviewClose}>
          <Grid container spacing={2}>
            <Grid item xs={10}>
              <DialogTitle sx={{
                "color": "#233E82",
                "backgroundColor": "#E6E9F1",
                "font-size": "40px",
                "font-weight": "bold",
              }}>
                New features available for DRT (preview)
              </DialogTitle>
            </Grid>
            <Grid item xs={2} sx={{"backgroundColor": "#E6E9F1"}}>
              <DialogActions>
                {props.isEdit ? <div>
                  <IconButton aria-label="delete" onClick={handleDelete}><DeleteIcon/></IconButton>
                  <IconButton aria-label="edit" onClick={handleEdit}><EditIcon/></IconButton>
                </div> : null}
                <IconButton aria-label="close"
                            onClick={handlePreviewClose}><CloseIcon/></IconButton>
              </DialogActions>
            </Grid>
          </Grid>
          <DialogContent sx={{
            "backgroundColor": "#E6E9F1",
            "padding-top": "0px",
            "padding-left": "24px",
            "padding-right": "24px",
            "padding-bottom": "64px",
            "overflow": "hidden"
          }}>
            <Grid container spacing={"2"}>
              <Grid item xs={8}
                    sx={{"backgroundColor": "#FFFFFF", "border": "16px solid #C0C7DE"}}>
                <video
                  src={props.videoURL}
                  width="100%"
                  height="100%"
                  controls
                />
              </Grid>
              <Grid item xs={4} sx={{
                "padding-left": "16px",
                "backgroundColor": "#FFFFFF",
                "border-top": "16px solid #C0C7DE",
                "border-right": "16px solid #C0C7DE",
                "border-bottom": "16px solid #C0C7DE",
                "border-left": "0px solid #C0C7DE"
              }}>
                <Grid container spacing={2} sx={{"padding": "16px"}}>
                  <Grid item xs={12} sx={{
                    "font": "Arial",
                    "font-size": "28px",
                    "font-weight": "bold",
                    "padding-bottom": "16px"
                  }}>
                    <div>{props.title}</div>
                  </Grid>
                  <Grid item xs={12} sx={{"font": "Arial", "padding-left": "16px"}}>
                    {props.markdownContent && props.markdownContent.split('\n').map((line, _) => (
                      <div>{line}</div>
                    ))}
                  </Grid>
                </Grid>
              </Grid>
            </Grid>
          </DialogContent>
        </Dialog>
      </div>
  )
}
