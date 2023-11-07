import React, {useEffect, useState} from 'react';
import {DataGrid, GridColDef, GridRenderCellParams, GridRowModel} from "@mui/x-data-grid";
import IconButton from "@mui/material/IconButton";
import DeleteIcon from "@mui/icons-material/Delete";
import {Button, Snackbar} from "@mui/material";
import Box from "@mui/material/Box";
import axios, {AxiosResponse} from "axios";
import {DialogActionComponent} from "./DialogActionComponent";
import {redirect, useParams} from "react-router-dom";
import {SeminarData} from "./DropInsList";
import {Alert} from "../DialogComponent";

export interface DropInRegisteredUsers {
  email: string;
  dropInId: string;
  registeredAt: string;
  emailSentAt: string;
}

export function RegisteredUsers() {
  const columns: GridColDef[] = [
    {
      field: 'dropInId',
      headerName: 'Id',
      width: 200,
    },
    {
      field: 'email',
      headerName: 'Email',
      width: 250,
    },
    {
      field: 'registeredAt',
      headerName: 'Registration Time',
      width: 150,
    },
    {
      field: 'emailSentAt',
      headerName: 'Email Sent Time',
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
  const {dropInId} = useParams<{ dropInId: string }>();
  const [rowsData, setRowsData] = React.useState([] as GridRowModel[]);
  const [rowDetails, setRowDetails] = React.useState({} as DropInRegisteredUsers | undefined)
  const [selectedRow, setSelectedRow] = React.useState<SeminarData | null>(null);
  const [error, setError] = useState(false);
  const [unregister, setUnregister] = useState(false);

  const handleRemove = (userData: DropInRegisteredUsers | undefined) => {
    setRowDetails(userData)
    setUnregister(true);
  }

  const handleSeminarResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      setSelectedRow(response.data)
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
    axios.get('/drop-in-register/users/' + dropInId)
      .then(response => handleResponse(response))
      .then(data => {
        console.log(data);
      }).catch(error => {
      setError(true);
      console.error(error);
    });
    axios.get('/drop-in/get/' + dropInId)
      .then(response => handleSeminarResponse(response))
      .then(data => {
        console.log(data);
      }).catch(error => {
      setError(true);
      console.error(error);
    });
  }, [dropInId, unregister]);

  const handleBack = () => {
    setError(false);
    redirect('/drop-ins/list');
  }

  return (
    <div>
      {<div>
        <Snackbar
          anchorOrigin={{vertical: 'top', horizontal: 'center'}}
          open={error}
          autoHideDuration={6000}
          onClose={() => setError(false)}>
          <Alert onClose={() => setError(false)} severity="error" sx={{width: '100%'}}>
            There was a problem booking drop-ins. Please try reloading the page.
          </Alert>
        </Snackbar>
        <h1>Drop-In registrations - {selectedRow?.title}</h1>
        <Box sx={{height: 400, width: '100%'}}>
          <DataGrid
            getRowId={(rowsData) => rowsData.email + '_' + rowsData.dropInId}
            rows={rowsData}
            columns={columns}
            pageSizeOptions={[5]}
          />
          <Button style={{float: 'right'}} variant="outlined"
                  color="primary"
                  onClick={handleBack}>back</Button>
        </Box>
        <DialogActionComponent actionString='unregister'
                               actionMethod='DELETE'
                               showDialog={unregister}
                               setShowDialog={setUnregister}
                               actionUrl={'/drop-in-register/remove/' + rowDetails?.dropInId + '/' + rowDetails?.email}
        />
      </div>
      } </div>
  );
}
