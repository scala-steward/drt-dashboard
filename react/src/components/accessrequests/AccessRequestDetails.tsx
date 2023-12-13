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
import ConfirmAccessRequest from "./ConfirmAccessRequest";
import {KeyCloakUser} from './AccessRequestCommon';
import moment from "moment-timezone";

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
  accessRequest: UserRequestedAccessData
  status: string;
  receivedUserDetails: boolean
  setReceivedUserDetails: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export default function AccessRequestDetails(props: IProps) {
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
    axios.get(ApiClient.userDetailsEndpoint + '/' + props.accessRequest.email)
      .then(response => updateState(response.data as KeyCloakUser))
  }

  const revertAccessRequest = () => {
    setMessage("Revert")
    axios.post(ApiClient.updateUserRequestEndpoint + "/" + "Requested", props.accessRequest)
      .then(() => setRequestPosted(true))
      .then(() => setReceivedUserDetails(false))
  }

  React.useEffect(() => {
    if (receivedUserDetails && (user.id)) {
      axios.post(ApiClient.addUserToGroupEndpoint + '/' + user.id, props.accessRequest)
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
            <TableContainer component={Paper} sx={{
              maxHeight: 500,
              overflowX: 'hidden',
              overflowY: 'auto',
            }}>
              <Table sx={{minWidth: 500}} size="small" aria-label="a dense table">
                <TableBody>
                  <TableRow>
                    <TableCell>Email</TableCell>
                    <TableCell>
                      <a href={"mailto:" + props.accessRequest.email + "?Subject=DRT%20access%20request"}
                         target="_blank">{props.accessRequest.email}</a>
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Line Manager</TableCell>
                    <TableCell>
                      <a href={"mailto:" + props.accessRequest.lineManager + "?Subject=DRT%20access%20request"}
                         target="_blank">{props.accessRequest.lineManager}</a>
                    </TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Requested at</TableCell>
                    <TableCell>{moment(props.accessRequest.requestTime).format("HH:mm, Do MMM YYYY")}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Account type</TableCell>
                    <TableCell>{props.accessRequest.accountType}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Ports</TableCell>
                    <TableCell>{props.accessRequest.allPorts ? 'All ports' : props.accessRequest.portsRequested}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Regions</TableCell>
                    <TableCell>{props.accessRequest.regionsRequested}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Staffing editing?</TableCell>
                    <TableCell>{props.accessRequest.staffEditing ? 'Yes' : 'No'}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Reason for ports/regions</TableCell>
                    <TableCell sx={{whiteSpace: 'pre-line'}}>{props.accessRequest.portOrRegionText}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Reason for staff editing</TableCell>
                    <TableCell sx={{whiteSpace: 'pre-line'}}>{props.accessRequest.staffText}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Request Status</TableCell>
                    <TableCell>{props.accessRequest.status}</TableCell>
                  </TableRow>
                  <TableRow>
                    <TableCell>Agree Declaration</TableCell>
                    <TableCell>{props.accessRequest.agreeDeclaration ? 'Yes' : 'No'}</TableCell>
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
      <ConfirmAccessRequest message={message}
                            parentRequestPosted={requestPosted}
                            setParentRequestPosted={setRequestPosted}
                            receivedUserDetails={props.receivedUserDetails}
                            setReceivedUserDetails={props.setReceivedUserDetails}
                            openModel={props.openModal}
                            setOpenModel={props.setOpenModal}
                            emails={[props.accessRequest.email ?? user.email]}/> : viewUserDetailTable()
  }

  return (
    viewPage()
  );

}
