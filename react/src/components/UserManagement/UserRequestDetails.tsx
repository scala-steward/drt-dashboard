import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import Modal from '@mui/material/Modal';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
// import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import Grid from "@mui/material/Grid";
import axios from "axios";
import ApiClient from "../../services/ApiClient";
import UserAccessApproved from "./UserAccessApproved";
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
    approvedPage: boolean;
}

export default function UserRequestDetails(props: IProps) {
    const [open, setOpen] = React.useState(true);
    const [requestPosted, setRequestPosted] = React.useState(false)
    const [userDetails, setUserDetails] = React.useState({});
    const [apiRequestCount, setApiRequestCount] = React.useState(0);

    const handleClose = () => {
        props.setOpenModal(false)
        setOpen(false)
    }

    const keyCloakUserDetails = () => {
        axios.get(ApiClient.userDetailsEndpoint + '/' + props.rowDetails?.email)
            .then(response => setUserDetails(response.data as KeyCloakUser))
            .then(() => setApiRequestCount(1))
    }

    React.useEffect(() => {
        if (apiRequestCount == 1) {
            axios.post(ApiClient.addUserToGroupEndpoint + '/' + (userDetails as KeyCloakUser).id, props.rowDetails)
                .then(response => console.log("User addUserToGroupEndpoint" + response.data))
                .then(() => setRequestPosted(true))
        }


    }, [userDetails]);

    const showApprovedButton = () => {
        return !props.approvedPage ?
            <Button style={{float: 'initial'}} variant="contained" color="primary"
                    onClick={keyCloakUserDetails}>Approve</Button>
            : <span/>
    }

    const viewUserDetailTable = () => {
        return <div className="flex-container">
            <div>
                <Modal
                    open={open}
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
                                        <TableCell>{props.rowDetails?.email}</TableCell>
                                    </TableRow>
                                    <TableRow>
                                        <TableCell>Line Manager</TableCell>
                                        <TableCell>{props.rowDetails?.lineManager}</TableCell>
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
                                {showApprovedButton()}
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
        return requestPosted ? <UserAccessApproved userDetails={userDetails as KeyCloakUser}/> : viewUserDetailTable()
    }

    return (
        viewPage()
    );

}