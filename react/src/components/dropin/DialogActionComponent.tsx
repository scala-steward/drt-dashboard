import React, {useEffect, useState} from "react";
import axios, {AxiosResponse} from "axios";
import {Alert, DialogComponent} from "../DialogComponent";
import {Snackbar} from "@mui/material";

interface Props {
    showDialog: boolean;
    setShowDialog: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
    actionUrl: string;
    actionString: string;
    actionMethod: string;
}

export function DialogActionComponent(props: Props) {
    const [error, setError] = useState(false);
    const [confirmAction, setConfirmAction] = useState(false);
    const handleResponse = (response: AxiosResponse) => {
        if (response.status === 200) {
            props.setShowDialog(false);
            console.log(props.actionString + ' Drop-In data');
        } else {
            setError(true);
            response.data
        }
    }

    useEffect(() => {
        handleConfirmDialog();
    }, [confirmAction]);

    const handleConfirmDialog = () => {
        if (confirmAction) {
            if (props.actionMethod == "DELETE")
                executeDeleteAction();
            else
                executePostAction();
        }

    }

    const executePostAction = () => {
        axios.post(props.actionUrl, {published: props.actionString == "publish"})
            .then(response => handleResponse(response))

    }

    const executeDeleteAction = () => {
        axios.delete(props.actionUrl)
            .then(response => handleResponse(response))
    }

    const handleErrorDialogClose = () => {
        setError(false);
        setConfirmAction(false);
    }


    const handleCloseSnackBar = () => {
        setError(false);
        setConfirmAction(false);
    }

    return (
        <div>
            <Snackbar
                anchorOrigin={{vertical: 'top', horizontal: 'center'}}
                open={error}
                autoHideDuration={6000}
                onClose={() => handleErrorDialogClose}>
                <Alert onClose={handleErrorDialogClose} severity="error" sx={{width: '100%'}}>
                    Error while {props.actionString} !
                </Alert>
            </Snackbar>
            <Snackbar
                anchorOrigin={{vertical: 'top', horizontal: 'center'}}
                open={confirmAction}
                autoHideDuration={6000}
                onClose={() => handleCloseSnackBar}>
                <Alert onClose={handleCloseSnackBar} severity="success" sx={{width: '100%'}}>
                    Requested is {props.actionString} !
                </Alert>
            </Snackbar>
            <DialogComponent displayText={props.actionString}
                             showDialog={props.showDialog}
                             setShowDialog={props.setShowDialog}
                             setConfirmAction={setConfirmAction}/>
        </div>

    )
}
