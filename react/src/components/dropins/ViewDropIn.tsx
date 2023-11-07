import React from "react";
import {Dialog, DialogActions, DialogContent, DialogTitle} from "@mui/material";
import Grid from "@mui/material/Grid";
import IconButton from "@mui/material/IconButton";
import CloseIcon from "@mui/icons-material/Close";
import Typography from "@mui/material/Typography";
import {stringToUKDate} from "./DropInsList";

interface Props {
    id: string | undefined;
    title: string | undefined;
    startTime: string | undefined;
    endTime: string | undefined;
    meetingLink: string | undefined;
    view: boolean;
    setView: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export function ViewDropIn(props: Props) {

    const handleViewClose = () => {
        props.setView(false)
    }

    return (
        <Dialog open={props.view} maxWidth="sm" onClose={handleViewClose}>
            <Grid container spacing={2}>
                <Grid item xs={8}>
                    <DialogTitle sx={{
                        "color": "#233E82",
                        "backgroundColor": "#E6E9F1",
                        "font-size": "30px",
                        "font-weight": "bold",
                    }}>
                        Drop-In View
                    </DialogTitle>
                </Grid>
                <Grid item xs={4} sx={{"backgroundColor": "#E6E9F1"}}>
                    <DialogActions>
                        <IconButton aria-label="close"
                                    onClick={handleViewClose}><CloseIcon/></IconButton>
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
                <Grid container spacing={3} alignItems="center">
                    <Grid item xs={3}>
                        <label>Drop-In Title:</label></Grid>
                    <Grid item xs={9}>
                        <Typography>{props.title}</Typography>
                    </Grid>
                    <Grid item xs={3}>
                        <label>Start Time:</label>
                    </Grid>
                    <Grid item xs={9}>
                        <Typography>{stringToUKDate(props.startTime)}</Typography>
                    </Grid>
                    <Grid item xs={3}>
                        <label>End Time:</label>
                    </Grid>
                    <Grid item xs={9}>
                        <Typography>{stringToUKDate(props.endTime)}</Typography>
                    </Grid>
                    <Grid item xs={3}>
                        <label>Meeting Link:</label>
                    </Grid>
                    <Grid item xs={9}>
                        <Typography><a href={props.meetingLink}>Team link</a></Typography>
                    </Grid>
                </Grid>
            </DialogContent>
        </Dialog>
    )
}
