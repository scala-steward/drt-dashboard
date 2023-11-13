import React, {useEffect, useState} from 'react';
import {DataGrid, GridColDef, GridRenderCellParams, GridRowModel} from "@mui/x-data-grid";
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import {Breadcrumbs, Snackbar, Stack} from "@mui/material";
import Box from "@mui/material/Box";
import axios, {AxiosResponse} from "axios";
import {Link, useParams} from "react-router-dom";
import {SeminarData} from "./DropInSessionsList";
import {Alert, DialogComponent} from "../DialogComponent";
import {enqueueSnackbar} from "notistack";
import ApiClient from "../../services/ApiClient";
import Typography from "@mui/material/Typography";

export interface DropInRegisteredUsers {
  email: string;
  dropInId: string;
  registeredAt: string;
  emailSentAt: string;
}

export function DropInSessionRegistrations() {
  const columns: GridColDef[] = [
    {
      field: 'email',
      headerName: 'Email',
      width: 250,
    },
    {
      field: 'registeredAt',
      headerName: 'Registered',
      width: 150,
    },
    {
      field: 'emailSentAt',
      headerName: 'Reminder Sent',
      width: 150,
    },
    {
      field: 'delete',
      headerName: 'Unregister',
      width: 100,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="delete">
          <DeleteIcon onClick={() => handleRemove(params.row as DropInRegisteredUsers)}/>
        </IconButton>
      ),
    },
  ];
  const {dropInId} = useParams<{ dropInId: string }>()
  const [rowsData, setRowsData] = React.useState([] as GridRowModel[])
  const [rowDetails, setRowDetails] = React.useState({} as DropInRegisteredUsers | undefined)
  const [dropInSession, setDropInSession] = React.useState<SeminarData | null>(null)
  const [error, setError] = useState(false)
  const [unregister, setUnregister] = useState(false)

  const handleRemove = (userData: DropInRegisteredUsers | undefined) => {
    setRowDetails(userData)
    setUnregister(true);
  }

  const handleDropInSessionResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      setDropInSession(response.data)
    } else {
      setError(true);
    }
  }

  const handleResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      setRowsData(response.data)
    } else {
      setError(true);
      response.data
    }
  }

  useEffect(() => {
    axios.get(`${ApiClient.dropInSessionRegistrationsEndpoint}/${dropInId}`)
      .then(response => handleResponse(response))
      .catch(error => {
        setError(true);
        console.error(error);
      });
    axios.get(`${ApiClient.getDropInSessionEndpoint}/${dropInId}`)
      .then(response => handleDropInSessionResponse(response))
      .catch(error => {
        setError(true);
        console.error(error);
      });
  }, [dropInId, unregister]);

  const removeRegistration = (sessionId: string, email: string) => {
    axios.delete(`${ApiClient.dropInSessionRegistrationDeleteEndpoint}/${sessionId}/${email}`)
      .then(response => {
        if (response.status === 200) {
          enqueueSnackbar('The user has been unregistered')
        } else
          enqueueSnackbar('There was a problem unregistering the user. Please try again later.')
      })
  }

  return <Stack gap={4} alignItems={'stretch'} sx={{mt: 2}}>
    <Breadcrumbs>
      <Link to={"/"}>
        Home
      </Link>
      <Link to={"/drop-in-sessions"}>
        Drop-in sessions
      </Link>
      <Typography color="text.primary">Registrations :: {dropInSession?.title}</Typography>
    </Breadcrumbs>
    <Snackbar
      anchorOrigin={{vertical: 'top', horizontal: 'center'}}
      open={error}
      autoHideDuration={6000}
      onClose={() => setError(false)}>
      <Alert onClose={() => setError(false)} severity="error" sx={{width: '100%'}}>
        There was a problem booking drop-ins. Please try reloading the page.
      </Alert>
    </Snackbar>
    <Box sx={{height: 400, width: '100%'}}>
      <DataGrid
        getRowId={(rowsData) => rowsData.email + '_' + rowsData.dropInId}
        rows={rowsData}
        columns={columns}
        pageSizeOptions={[5]}
      />
    </Box>
    {unregister && <DialogComponent actionText='unregister'
                                    onCancel={() => setUnregister(false)}
                                    onConfirm={() => {
                                      removeRegistration(rowDetails?.dropInId as string, rowDetails?.email as string)
                                    }}
    />}
  </Stack>
}
