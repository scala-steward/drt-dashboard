import Box from "@mui/material/Box";
import {DataGrid, GridColDef, GridRowModel} from "@mui/x-data-grid";
import * as React from "react";
import axios, {AxiosResponse} from "axios";
import ApiClient from "../../services/ApiClient";
import {GridValueFormatterParams} from "@mui/x-data-grid/models/params/gridCellParams";
import moment from "moment-timezone";

const formatDate = (param:GridValueFormatterParams) => {
    return moment(param?.value).format("YYYY-MM-DD HH:mm")}

export const userColumns: GridColDef[] = [
    {
        field: 'id',
        headerName: 'Id',
        width: 100,
        hide: true
    },
    {
        field: 'username',
        headerName: 'Username',
        width: 150,
    },
    {
        field: 'email',
        headerName: 'Email',
        width: 280
    },
    {
        field: 'latest_login',
        headerName: 'Latest Login',
        width: 200,
        valueFormatter: params =>
            params.value ? formatDate(params) : '',
    },
    {
        field: 'inactive_email_sent',
        headerName: 'Inactive Email Sent',
        description: 'This column has a value getter and is not sortable.',
        sortable: false,
        width: 200,
        valueFormatter: params =>
            params.value ? formatDate(params) : '',
    },
    {
        field: 'revoked_access',
        headerName: 'Revoked Access',
        width: 200,
        valueFormatter: params =>
            params.value ? formatDate(params) : '',
    },

];

export default function UserTracking() {
    const [rowsData, setRowsData] = React.useState([] as GridRowModel[]);
    const [userRequested, setUserRequested] = React.useState(false);

    const handleAccessRequestsResponse = (response: AxiosResponse) => {
        setRowsData(response.data as GridRowModel[])
    }

    const requestAccessRequests = () => {
        setUserRequested(true)
        axios.get(ApiClient.userListEndpoint)
            .then(response => handleAccessRequestsResponse(response))

    }

    React.useEffect(() => {
        if (!userRequested) {
            requestAccessRequests();
        }
    },[userRequested]);


    return (
        <Box sx={{height: 400, width: '100%'}}>
            <DataGrid
                rows={rowsData}
                columns={userColumns}
                pageSize={5}
                rowsPerPageOptions={[5]}
                experimentalFeatures={{newEditingApi: true}}
            />
        </Box>
    )
}
