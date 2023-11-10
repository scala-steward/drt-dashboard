import React, {useState} from "react";
import {Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import {useNavigate} from "react-router-dom";
import {DialogComponent} from "../DialogComponent";
import {deleteFeatureGuide} from "../../store/featureGuides";
import {FeatureData} from "./FeatureGuidesList";

interface Props {
  guide: FeatureData
  videoUrl: string
  onClose: () => void
  actionsAvailable: boolean
}

export function PreviewComponent(props: Props) {
  const navigate = useNavigate();
  const [deleteId, setDeleteId] = useState<string | undefined>(undefined);

  return <div>
    {deleteId && <DialogComponent actionText={'delete'}
                                  onConfirm={() => {
                                    deleteFeatureGuide(props.guide.id)
                                    props.onClose()
                                  }}
                                  onCancel={() => setDeleteId(undefined)}/>}
    <Dialog open={true} maxWidth="lg" onClose={props.onClose}>
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
            {props.actionsAvailable &&
                <IconButton aria-label="delete" onClick={() => setDeleteId(props.guide.id)}><DeleteIcon/></IconButton>}
            {props.actionsAvailable && <IconButton aria-label="edit"
                                                   onClick={() => navigate(`/feature-guides/edit/${props.guide.id}`)}><EditIcon/></IconButton>}
            <IconButton aria-label="close" onClick={props.onClose}><CloseIcon/></IconButton>
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
              src={props.videoUrl}
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
                <div>{props.guide.title}</div>
              </Grid>
              <Grid item xs={12} sx={{"font": "Arial", "padding-left": "16px"}}>
                {props.guide.markdownContent && props.guide.markdownContent
                  .split('\n')
                  .map(line => <div>{line}</div>)}
              </Grid>
            </Grid>
          </Grid>
        </Grid>
      </DialogContent>
    </Dialog>
  </div>
}
