import Box from "@mui/material/Box";
import {DataGrid, GridColDef, GridRowModel} from "@mui/x-data-grid";
import * as React from "react";
import {useEffect} from "react";
import axios from "axios";
import ApiClient from "../../services/ApiClient";
import {GridValueFormatterParams} from "@mui/x-data-grid/models/params/gridCellParams";
import moment from "moment-timezone";
import {Breadcrumbs} from "@mui/material";
import Typography from "@mui/material/Typography";
import Link from "@mui/material/Link";

const formatDate = (param: GridValueFormatterParams) => {
  return moment(param?.value).format("YYYY-MM-DD HH:mm")
}

export const userColumns: GridColDef[] = [
  {
    field: 'id',
    headerName: 'Id',
    width: 100,
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

export default function UsersList() {
  const [rowsData, setRowsData] = React.useState([] as GridRowModel[]);

  useEffect(() => {
    const fetchUsers = () =>
      axios
        .get(ApiClient.userListEndpoint)
        .then(response => setRowsData(response.data as GridRowModel[]))

    fetchUsers();
  }, []);

  return <>
    <Breadcrumbs>
      <Link underline="hover" color="inherit" href="/">
        MUI
      </Link>
      <Link
        underline="hover"
        color="inherit"
        href="/material-ui/getting-started/installation/"
      >
        Core
      </Link>
      <Typography color="text.primary">Breadcrumbs</Typography>
    </Breadcrumbs>
    <Box sx={{height: 400, width: '100%'}}>
      <DataGrid
        rows={rowsData}
        columns={userColumns}
        pageSizeOptions={[5]}
      />
    </Box>
  </>
}
