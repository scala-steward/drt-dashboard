import * as React from 'react';
import Box from '@mui/material/Box';
import {DataGrid, GridRowId, GridRowModel} from '@mui/x-data-grid';
import ApiClient from "../../services/ApiClient";
import axios, {AxiosResponse} from "axios";
import AccessRequestDetails, {UserRequestedAccessData} from "./AccessRequestDetails";
import ConfirmAccessRequest from "./ConfirmAccessRequest";
import AccessRequestStatusList from "./AccessRequestStatusList";
import {Button, Stack, Typography, Breadcrumbs} from "@mui/material";
import { Link } from 'react-router-dom';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import {columns, KeyCloakUser} from "./AccessRequestCommon";
import {GridRowSelectionModel} from "@mui/x-data-grid/models/gridRowSelectionModel";
import { Helmet } from 'react-helmet';
import {adminPageTitleSuffix} from "../../utils/common";
import PageContentWrapper from '../PageContentWrapper';

export default function AccessRequests() {
  const [accessRequestListRequested, setAccessRequestListRequested] = React.useState(false);
  const [userRequestList, setUserRequestList] = React.useState([] as UserRequestedAccessData[]);
  const [rowsData, setRowsData] = React.useState([] as GridRowModel[]);
  const [receivedUserDetails, setReceivedUserDetails] = React.useState(false);
  const [openModal, setOpenModal] = React.useState(false)
  const [rowDetails, setRowDetails] = React.useState({} as UserRequestedAccessData | undefined)
  const [selectedRowDetails, setSelectedRowDetails] = React.useState([] as UserRequestedAccessData[]);
  const [selectedRowIds, setSelectedRowIds] = React.useState<GridRowId[]>([]);
  const [users, setUsers] = React.useState([] as KeyCloakUser[]);
  const [requestPosted, setRequestPosted] = React.useState(false)
  const [dismissedPosted, setDismissedPosted] = React.useState(false)
  const [statusFilterValue, setStatusFilterValue] = React.useState("Requested")

  const handleChange = (event: React.SyntheticEvent, newValue: string) => {
    setReceivedUserDetails(false)
    setStatusFilterValue(newValue);
    setAccessRequestListRequested(false);
  };

  const handleAccessRequestsResponse = (response: AxiosResponse) => {
    setUserRequestList(response.data as UserRequestedAccessData[])
    setRowsData(response.data as GridRowModel[])
  }

  const approveAndUpdateUserDetailsState = (response: AxiosResponse) => {
    approveUserAccessRequest(response.data as KeyCloakUser)
    setUsers(oldUsers => [...oldUsers, response.data as KeyCloakUser]);
  }

  const approveAndUpdateKeyCloakUserDetails = (emails: string[]) => {
    emails.map(email => axios.get(ApiClient.userDetailsEndpoint + '/' + email)
      .then(response => approveAndUpdateUserDetailsState(response)))
  }

  const requestAccessRequests = () => {
    setReceivedUserDetails(true)
    axios.get(ApiClient.requestAccessEndPoint + '?status=Requested')
      .then(response => handleAccessRequestsResponse(response))
  }

  const findAccessRequestByRowId = (requestTime: string | number) => {
    return userRequestList.find(obj => {
      return obj.requestTime.trim() == requestTime
    });
  }

  const rowClickOpen = (userData: UserRequestedAccessData | undefined) => {
    setRowDetails(userData)
    setOpenModal(true)
  }

  const approveSelectedUserRequests = () => {
    setUsers([{} as KeyCloakUser]);

    const emails: string[] = selectedRowIds
      .map(s => findAccessRequestByRowId(s)?.email)
      .filter((s): s is string => !!s);

    approveAndUpdateKeyCloakUserDetails(emails)
  }

  const addSelectedRowDetails = (srd: UserRequestedAccessData) => {
    setSelectedRowDetails(oldSelectedRowDetails => [...oldSelectedRowDetails, srd])
  }

  const addSelectedRows = (ids: GridRowSelectionModel) => {
    console.log(ids);
    setSelectedRowDetails([])
    if (ids) {
      ids.map(id => findAccessRequestByRowId(id))
        .filter((s): s is UserRequestedAccessData => !!s)
        .map(s => addSelectedRowDetails(s))
    }
    setSelectedRowIds(ids)
  }

  const findRequestByEmail = (email: string) => {
    return userRequestList.find(sr => sr.email == email)
  }

  const approveUserAccessRequest = (user: KeyCloakUser) => {
    const email = findRequestByEmail((user as KeyCloakUser).email)
    if (email) {
      axios.post(ApiClient.addUserToGroupEndpoint + '/' + (user as KeyCloakUser).id, email)
        .then(response => console.log("User addUserToGroupEndpoint" + response))
        .then(() => setRequestPosted(true))
    }
  }

  React.useEffect(() => {
    if (!receivedUserDetails) {
      requestAccessRequests();
    }
  }, [receivedUserDetails]);

  const viewSelectAccessRequest = () => {
    return <Box sx={{height: 400, width: '100%'}}>
      <DataGrid
        getRowId={(rowsData) => rowsData.requestTime}
        rows={rowsData}
        columns={columns}
        pageSizeOptions={[5]}
        checkboxSelection={true}
        disableRowSelectionOnClick
        onRowSelectionModelChange={addSelectedRows}
        onRowClick={(params, event: any) => {
          if (!event.ignore) {
            rowClickOpen(findAccessRequestByRowId(params.row.requestTime));
          }
        }}
      />
      {(openModal && rowDetails) ? <AccessRequestDetails openModal={openModal}
                                                         setOpenModal={setOpenModal}
                                                         accessRequest={rowDetails}
                                                         receivedUserDetails={receivedUserDetails}
                                                         setReceivedUserDetails={setReceivedUserDetails}
                                                         status={""}/> : <span/>
      }

      <Stack gap={1} direction={'row'} sx={{my: 2}}>
        <Button variant="outlined" disabled={selectedRowIds.length === 0}
                onClick={approveSelectedUserRequests}>Approve</Button>
        <Button variant="outlined" disabled={selectedRowIds.length === 0}
                onClick={dismissSelectedAccessRequests}>Dismiss</Button>
      </Stack>
    </Box>
  }

  const dismissSelectedAccessRequests = () => {
    selectedRowDetails
      .map(selectedRowDetail => axios.post(ApiClient.updateUserRequestEndpoint + "/" + "Dismissed", selectedRowDetail)
        .then(response => console.log('dismiss user' + response.data)))
    setDismissedPosted(true)
  }

  const showDismissedRequest = () => {
    return dismissedPosted ?
      <ConfirmAccessRequest message={"dismissed"}
                            parentRequestPosted={dismissedPosted}
                            setParentRequestPosted={setDismissedPosted}
                            receivedUserDetails={receivedUserDetails}
                            setReceivedUserDetails={setReceivedUserDetails}
                            openModel={openModal}
                            setOpenModel={setOpenModal}
                            emails={selectedRowDetails.map(srd => srd.email)}/> : viewSelectAccessRequest()
  }

  const showApprovedOrAccessRequest = () => {
    return requestPosted ?
      <ConfirmAccessRequest message={"granted"}
                            parentRequestPosted={requestPosted}
                            setParentRequestPosted={setRequestPosted}
                            receivedUserDetails={receivedUserDetails}
                            setReceivedUserDetails={setReceivedUserDetails}
                            openModel={openModal}
                            setOpenModel={setOpenModal}
                            emails={users.map(ud => ud.email)}/> : showDismissedRequest()
  }

  const accessRequestOrApprovedList = () => {
    switch (statusFilterValue) {
      case "Approved" :
        return <AccessRequestStatusList accessRequestListRequested={accessRequestListRequested}
                                        setAccessRequestListRequested={setAccessRequestListRequested}
                                        statusView={"Approved"}
                                        showUserRequestByStatus={statusFilterValue}
                                        setShowUserRequestByStatus={setStatusFilterValue}/>
      case "Dismissed" :
        return <AccessRequestStatusList accessRequestListRequested={accessRequestListRequested}
                                        setAccessRequestListRequested={setAccessRequestListRequested}
                                        statusView={"Dismissed"}
                                        showUserRequestByStatus={statusFilterValue}
                                        setShowUserRequestByStatus={setStatusFilterValue}/>
      case "Requested" :
        return showApprovedOrAccessRequest();
    }
  }

  return <PageContentWrapper>
    <Helmet>
      <title>Access requests {adminPageTitleSuffix}</title>
    </Helmet>
    <Stack gap={4} alignItems={'stretch'} sx={{mt: 2, mb: 10}}>
      <Breadcrumbs>
        <Link to={"/"}>
          Home
        </Link>
        <Typography color="text.primary">Access requests</Typography>
      </Breadcrumbs>
      <Box sx={{width: '100%'}}>
        <Tabs
          value={statusFilterValue}
          onChange={handleChange}
          textColor="secondary"
          indicatorColor="secondary"
          aria-label="secondary tabs example">
          <Tab value="Requested" label="Requested Access"/>
          <Tab value="Approved" label="Approved Access"/>
          <Tab value="Dismissed" label="Dismissed Access"/>
        </Tabs>
        <div> {accessRequestOrApprovedList()} </div>
      </Box>
    </Stack>
  </PageContentWrapper>
}
