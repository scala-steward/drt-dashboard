import * as React from 'react';
import Box from '@mui/material/Box';
import {DataGrid, GridRowModel} from '@mui/x-data-grid';
import ApiClient from "../../services/ApiClient";
import axios, {AxiosResponse} from "axios";
import UserRequestDetails, {UserRequestedAccessData} from "./UserRequestDetails";
import {Button} from "@mui/material";
import {columns} from "./UserAccessCommon";

interface IProps {
  accessRequestListRequested: boolean
  setAccessRequestListRequested: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  statusView: string;
  showUserRequestByStatus: string;
  setShowUserRequestByStatus: ((value: (((prevState: string) => string) | string)) => void);
}

export default function UserRequestStatusList(props: IProps) {
  const [userRequestList, setUserRequestList] = React.useState([] as UserRequestedAccessData[]);
  const [rowsData, setRowsData] = React.useState([] as GridRowModel[]);
  const [rowDetails, setRowDetails] = React.useState({} as UserRequestedAccessData | undefined)
  const [openModal, setOpenModal] = React.useState(false)

  const updateAccessRequestData = (response: AxiosResponse) => {
    setUserRequestList(response.data as UserRequestedAccessData[])
    setRowsData(response.data as GridRowModel[])
  }

  const requestAccessRequestsWithStatus = () => {
    props.setAccessRequestListRequested(true)
    axios.get(ApiClient.requestAccessEndPoint + '?status=' + props.statusView)
      .then(response => updateAccessRequestData(response))
  }

  const resetUserRequestShowStatus = () => {
    props.setShowUserRequestByStatus("Requested")
  }

  React.useEffect(() => {
    if (!props.accessRequestListRequested) {
      requestAccessRequestsWithStatus();
    }
  });

  const findEmail = (requestTime: string) => {
    return userRequestList.find(obj => {
      return obj.requestTime.trim() == requestTime
    });
  }

  const rowClickOpen = (userData: UserRequestedAccessData | undefined) => {
    setRowDetails(userData)
    setOpenModal(true)
  }

  return (
    <Box sx={{height: 400, width: '100%'}}>
      <DataGrid
        getRowId={(rowsData) => rowsData.requestTime}
        rows={rowsData}
        columns={columns}
        pageSizeOptions={[5]}
        onRowClick={(params, event: any) => {
          if (!event.ignore) {
            rowClickOpen(findEmail(params.row.requestTime));
          }
        }}
      />
      {
        (openModal) ?
          <UserRequestDetails openModal={openModal}
                              setOpenModal={setOpenModal}
                              receivedUserDetails={props.accessRequestListRequested}
                              setReceivedUserDetails={props.setAccessRequestListRequested}
                              rowDetails={rowDetails}
                              status={props.statusView}/> :
          <span/>
      }
      <Button style={{float: 'right'}}
              variant="outlined"
              color="primary"
              onClick={resetUserRequestShowStatus}>back</Button>
    </Box>
  );
}
