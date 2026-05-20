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
  const [failedApprovalEmails, setFailedApprovalEmails] = React.useState([] as string[]);
  const [dismissedEmails, setDismissedEmails] = React.useState([] as string[]);
  const [failedDismissalEmails, setFailedDismissalEmails] = React.useState([] as string[]);
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

  const approveUserAccessRequest = async (email: string): Promise<KeyCloakUser | null> => {
    const accessRequest = findRequestByEmail(email)

    if (!accessRequest) {
      return null
    }

    try {
      const response = await axios.get(ApiClient.userDetailsEndpoint + '/' + email)
      const user = response.data as KeyCloakUser

      await axios.post(ApiClient.addUserToGroupEndpoint + '/' + user.id, accessRequest)

      return user
    } catch (error) {
      console.error(`Failed to approve access request for ${email}`, error)
      return null
    }
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

  const selectedEmails = (): string[] => selectedRowIds
    .map(s => findAccessRequestByRowId(s)?.email)
    .filter((s): s is string => !!s)

  const actionResults = async <T,>(tasks: {email: string, task: Promise<T | null>}[]): Promise<{successful: T[], failedEmails: string[]}> => {
    const results = await Promise.all(tasks.map(async ({email, task}) => ({email, value: await task})))

    return results.reduce<{successful: T[], failedEmails: string[]}>((outcome, result) => result.value === null
      ? {...outcome, failedEmails: [...outcome.failedEmails, result.email]}
      : {...outcome, successful: [...outcome.successful, result.value]}, {successful: [], failedEmails: []})
  }

  const approveSelectedUserRequests = async () => {
    setUsers([]);
    setFailedApprovalEmails([])

    const {successful: approvedUsers, failedEmails} = await actionResults(selectedEmails().map(email => ({
      email,
      task: approveUserAccessRequest(email),
    })))

    setUsers(approvedUsers)
    setFailedApprovalEmails(failedEmails)
    setRequestPosted(approvedUsers.length + failedEmails.length > 0)
  }

  const addSelectedRowDetails = (srd: UserRequestedAccessData) => {
    setSelectedRowDetails(oldSelectedRowDetails => [...oldSelectedRowDetails, srd])
  }

  const addSelectedRows = (ids: GridRowSelectionModel) => {
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

  const dismissSelectedAccessRequests = async () => {
    setDismissedEmails([])
    setFailedDismissalEmails([])

    const {successful: successfulDismissals, failedEmails} = await actionResults(selectedRowDetails.map(selectedRowDetail => ({
      email: selectedRowDetail.email,
      task: (async () => {
        try {
          await axios.post(ApiClient.updateUserRequestEndpoint + "/" + "Dismissed", selectedRowDetail)
          return selectedRowDetail.email
        } catch (error) {
          console.error(`Failed to dismiss access request for ${selectedRowDetail.email}`, error)
          return null
        }
      })(),
    })))

    setDismissedEmails(successfulDismissals)
    setFailedDismissalEmails(failedEmails)
    setDismissedPosted(successfulDismissals.length + failedEmails.length > 0)
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
                            emails={dismissedEmails}
                            failedEmails={failedDismissalEmails}/> : viewSelectAccessRequest()
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
                            emails={users.map(ud => ud.email)}
                            failedEmails={failedApprovalEmails}/> : showDismissedRequest()
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
