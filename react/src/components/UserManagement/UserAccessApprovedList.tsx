import * as React from 'react';
import Box from '@mui/material/Box';
import {DataGrid, GridRowModel} from '@mui/x-data-grid';
import ApiClient from "../../services/ApiClient";
import axios, {AxiosResponse} from "axios";
import UserRequestDetails, {UserRequestedAccessData} from "./UserRequestDetails";
import {Button} from "@mui/material";
import {columns} from "./UserAccessCommon";

interface IProps {
    showApprovedUserRequest: string;
    setShowApprovedUserRequest: ((value: (((prevState: string) => string) | string)) => void);
}

export default function UserAccessApprovedList(props: IProps) {
    const [userRequestList, setUserRequestList] = React.useState([] as UserRequestedAccessData[]);
    const [rowsData, setRowsData] = React.useState([] as GridRowModel[]);
    const [apiRequestCount, setApiRequestCount] = React.useState(0);
    const [rowDetails, setRowDetails] = React.useState({} as UserRequestedAccessData | undefined)
    const [openModal, setOpenModal] = React.useState(false)

    const updateAccessRequestData = (response: AxiosResponse) => {
        setUserRequestList(response.data as UserRequestedAccessData[])
        setRowsData(response.data as GridRowModel[])
    }

    const approvedAccessApi = () => {
        setApiRequestCount(1)
        axios.get(ApiClient.requestAccessEndPoint + '?status=Approved')
            .then(response => updateAccessRequestData(response))
            .then(() => console.log("User request response"))
    }

    const resetApprovedUserRequest = () => {
        props.setShowApprovedUserRequest("Requested")
    }

    React.useEffect(() => {
        if (apiRequestCount == 0) {
            approvedAccessApi();
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
        console.log('rowClickOpen ' + openModal)
    }

    return (
        <Box sx={{height: 400, width: '100%'}}>
            <DataGrid
                getRowId={(rowsData) => rowsData.requestTime}
                rows={rowsData}
                columns={columns}
                pageSize={5}
                rowsPerPageOptions={[5]}
                experimentalFeatures={{newEditingApi: true}}
                onRowClick={(params, event: any) => {
                    if (!event.ignore) {
                        rowClickOpen(findEmail(params.row.requestTime));
                    }
                }}
            />
            {
                (openModal) ?
                    <UserRequestDetails openModal={openModal} setOpenModal={setOpenModal} rowDetails={rowDetails}
                                        approvedPage={true}/> :
                    <span/>
            }
            <Button style={{float: 'right'}} variant="outlined" color="primary" onClick={resetApprovedUserRequest}>back</Button>
        </Box>

    );
}
