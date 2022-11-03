import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import Modal from '@mui/material/Modal';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Grid from "@mui/material/Grid";
import axios from "axios";
import ApiClient from "../../services/ApiClient";
import ConfirmUserAccess from "./ConfirmUserAccess";
import {KeyCloakUser} from './UserAccessCommon';

export interface UserRequestedAccessData {
    agreeDeclaration: boolean;
    allPorts: boolean;
    email: string;
    lineManager: string;
    portOrRegionText: string;
    portsRequested: string;
    accountType: string;
    regionsRequested: string;
    requestTime: string;
    staffText: string;
    staffEditing: boolean;
    status: string
}

const style = {
    position: 'absolute' as 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 600,
    bgcolor: 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
};

interface IProps {
    openModal: boolean;
    setOpenModal: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
    rowDetails: UserRequestedAccessData | undefined
    status: string;
    receivedUserDetails: boolean
    setReceivedUserDetails: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export default function UserRequestDetails(props: IProps) {
    const [requestPosted, setRequestPosted] = React.useState(false)
    const [user, setUser] = React.useState({} as KeyCloakUser);
    const [receivedUserDetails, setReceivedUserDetails] = React.useState(false);
    const [message, setMessage] = React.useState("");
    const handleClose = () => {
        props.setOpenModal(false)
        setReceivedUserDetails(false)
    }

    const updateState = (keyCloakUser: KeyCloakUser) => {
        setReceivedUserDetails(true)
        setUser(keyCloakUser)
    }

    const keyCloakUserDetails = () => {
        setMessage("Granted")
        axios.get(ApiClient.userDetailsEndpoint + '/' + props.rowDetails?.email)
            .then(response => updateState(response.data as KeyCloakUser))
    }

    const revertAccessRequest = () => {
        setMessage("Revert")
        axios.post(ApiClient.updateUserRequestEndpoint + "/" + "Requested", props.rowDetails)
            .then(response => console.log('reverted user' + response.data))
            .then(() => setRequestPosted(true))
            .then(() => setReceivedUserDetails(false))
    }

    React.useEffect(() => {
        console.log('React.useEffect apiRequestCount ' + receivedUserDetails)
        if (receivedUserDetails) {
            axios.post(ApiClient.addUserToGroupEndpoint + '/' + (user as KeyCloakUser).id, props.rowDetails)
                .then(response => console.log("User addUserToGroupEndpoint" + response.data))
                .then(() => setRequestPosted(true))
                .then(() => setReceivedUserDetails(false))
        }
        if (!props.receivedUserDetails) {
            props.setOpenModal(false)
        }
    }, [user, receivedUserDetails]);

    const accessButton = () => {
        switch (props.status) {
            case "Approved" :
                return <span/>

            case "Dismissed" :
                return <Button style={{float: 'initial'}} onClick={revertAccessRequest}>Revert</Button>

            case "" :
                return <Button style={{float: 'initial'}} onClick={keyCloakUserDetails}>Approve</Button>
        }
    }

    const viewUserDetailTable = () => {
        return <div className="flex-container">
            <div>
                <Modal
                    open={props.openModal}
                    onClose={handleClose}
                    aria-labelledby="modal-modal-title"
                    aria-describedby="modal-modal-description">
                    <Box sx={style}>
                        <Typography align="center" id="modal-modal-title" variant="h6" component="h2">
                            User Request Details
                        </Typography>
                        <TableContainer component={Paper}>
                            <Table sx={{minWidth: 500}} size="small" aria-label="a dense table">
                                <TableBody>
                                    <TableRow>
                                        <TableCell>Email</TableCell>
                                        <TableCell>
                                            <a href={"mailto:" + props.rowDetails?.email + "?Subject=DRT%20access%20request"}
                                               target="_blank">{props.rowDetails?.email}</a>
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Line Manager</TableCell>
                                        <TableCell>
                                            <a href={"mailto:" + props.rowDetails?.lineManager + "?Subject=DRT%20access%20request"}
                                               target="_blank">{props.rowDetails?.lineManager}</a>
                                        </TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>All ports requested</TableCell>
                                        <TableCell>{String(props.rowDetails?.allPorts)}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Time Requested</TableCell>
                                        <TableCell>{props.rowDetails?.requestTime}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Ports Request</TableCell>
                                        <TableCell>{props.rowDetails?.portsRequested}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Regions Request</TableCell>
                                        <TableCell>{props.rowDetails?.regionsRequested}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Staffing Requested</TableCell>
                                        <TableCell>{String(props.rowDetails?.staffEditing)}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Ports / Region Request reason</TableCell>
                                        <TableCell>{props.rowDetails?.portOrRegionText}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Staffing Reason</TableCell>
                                        <TableCell>{props.rowDetails?.staffText}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Rcc request</TableCell>
                                        <TableCell>{props.rowDetails?.accountType}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Request Status</TableCell>
                                        <TableCell>{props.rowDetails?.status}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Agree Declaration</TableCell>
                                        <TableCell>{String(props.rowDetails?.agreeDeclaration)}</TableCell>
                                    </TableRow>
                                </TableBody>
                            </Table>
                        </TableContainer>
                        <Grid container>
                            <Grid xs={8}>
                                {accessButton()}
                            </Grid>
                            <Grid xs={4}>
                                <Button style={{float: 'right'}} onClick={handleClose}>Close</Button>
                            </Grid>
                        </Grid>
                    </Box>
                </Modal>
            </div>
        </div>
    }

    const viewPage = () => {
        return requestPosted ?
            <ConfirmUserAccess message={message}
                               parentRequestPosted={requestPosted}
                               setParentRequestPosted={setRequestPosted}
                               receivedUserDetails={props.receivedUserDetails}
                               setReceivedUserDetails={props.setReceivedUserDetails}
                               openModel={props.openModal}
                               setOpenModel={props.setOpenModal}
                               emails={[props.rowDetails?.email ?? user.email]}/> : viewUserDetailTable()
    }

    return (
        viewPage()
    );

}