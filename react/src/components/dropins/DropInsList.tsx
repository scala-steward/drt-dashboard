import React, {useEffect, useState} from "react";
import {DataGrid, GridColDef, GridRenderCellParams, GridRowModel} from "@mui/x-data-grid";
import IconButton from "@mui/material/IconButton";
import PublishIcon from "@mui/icons-material/Publish";
import UnpublishedIcon from "@mui/icons-material/Unpublished";
import PreviewIcon from "@mui/icons-material/Preview";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import axios, {AxiosResponse} from "axios";
import {Breadcrumbs, FormControlLabel, Snackbar, Stack} from "@mui/material";
import Box from "@mui/material/Box";
import {ViewDropIn} from "./ViewDropIn";
import {CalendarViewMonth} from "@mui/icons-material";
import {DialogActionComponent} from "./DialogActionComponent";
import moment from 'moment-timezone';
import {Link} from "react-router-dom";
import {Alert} from "../DialogComponent";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";

export function stringToUKDate(date?: string): string | undefined {
  if (!date) {
    return undefined;
  }

  const parsedDate = moment.utc(date, "YYYY-MM-DD HH:mm:ss");

  if (!parsedDate.isValid()) {
    console.error("Invalid date provided:", date);
    return undefined;
  }

  const ukDatetime = parsedDate.tz("Europe/London");
  const result = ukDatetime.format('DD-MM-YYYY HH:mm');

  console.log("Input (UTC):", date);
  console.log("Converted to Europe/London:", result);

  return result;
}

export interface SeminarData {
  id: string;
  title: string;
  startTime: string;
  endTime: string;
  meetingLink: string;
  isPublished: boolean;
}

export function DropInsList() {

  const dropInColumns: GridColDef[] = [
    {
      field: 'id',
      headerName: 'Id',
      width: 50
    },
    {
      field: 'title',
      headerName: 'Title',
      width: 150,
    },
    {
      field: 'startTime',
      headerName: 'Start Time',
      width: 150,
      renderCell: (params) => {
        return <div>{stringToUKDate(params.value)}</div>;
      }
    },
    {
      field: 'endTime',
      headerName: 'End Time',
      width: 150,
      renderCell: (params) => {
        return <div>{stringToUKDate(params.value)}</div>;
      }
    },
    {
      field: 'meetingLink',
      headerName: 'Meeting Link',
      description: 'This column has a value getter and is not sortable.',
      width: 100,
      renderCell: (params: GridRenderCellParams) => (
        <a href={params.row.meetingLink} target="_blank">Team link</a>
      ),
    },
    {
      field: 'lastUpdatedAt',
      headerName: 'Updated',
      description: 'This column has a value getter and is not sortable.',
      width: 150,
      renderCell: (params) => {
        return <div>{stringToUKDate(params.value)}</div>;
      }
    },
    {
      field: 'isPublished',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="publish">
          {params.row.isPublished === true ?
            <PublishIcon onClick={() => handleUnPublish(params.row as SeminarData)}/> :
            <UnpublishedIcon onClick={() => handlePublish(params.row as SeminarData)}/>}
        </IconButton>
      ),
    },
    {
      field: 'view',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="view">
          <PreviewIcon onClick={() => rowClickOpen(params.row as SeminarData)}/>
        </IconButton>
      ),
    },
    {
      field: 'delete',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="delete">
          <DeleteIcon onClick={() => handleDelete(params.row as SeminarData)}/>
        </IconButton>
      ),
    },
    {
      field: 'edit',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <Link to={`/drop-ins/edit/${params.row.id}`}>
          <IconButton aria-label="edit">
            <EditIcon/>
          </IconButton>
        </Link>
      ),
    },
    {
      field: 'users',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <Link to={`/drop-ins/list/registeredUsers/${params.row.id}`}>
          <IconButton aria-label="users-registered">
            <CalendarViewMonth/>
          </IconButton>
        </Link>
      ),
    },
  ];

  const [rowsData, setRowsData] = React.useState([] as GridRowModel[]);
  const [error, setError] = useState(false);
  const [showDelete, setShowDelete] = useState(false);
  const [publish, setPublish] = useState(false);
  const [unPublish, setUnPublish] = useState(false);
  const [view, setView] = useState(false);
  const [rowDetails, setRowDetails] = React.useState({} as SeminarData | undefined);
  const [showAll, setShowAll] = useState(false);
  const [saved, setSaved] = useState(false);
  const [edited, setEdited] = useState(false);

  const handlePublish = (userData: SeminarData | undefined) => {
    setEdited(false)
    setSaved(false)
    setRowDetails(userData)
    setPublish(true);
  }

  const handleUnPublish = (userData: SeminarData | undefined) => {
    setEdited(false)
    setSaved(false)
    setRowDetails(userData)
    setUnPublish(true);
  }

  const handleDelete = (userData: SeminarData | undefined) => {
    setEdited(false)
    setSaved(false)
    setRowDetails(userData)
    setShowDelete(true);
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
    // if (operationsParam === 'saved') {
    //   setSaved(true);
    // }
    // if (operationsParam === 'edited') {
    //   setEdited(true);
    // }
    axios.get('/drop-in/getList/' + showAll)
      .then(response => handleResponse(response))
      .then(data => {
        console.log(data);
      }).catch(error => {
      setError(true);
      console.error(error);
    });
  }, [showAll, unPublish, publish, showDelete]);

  const handleBack = () => {
    setError(false);
  }

  const rowClickOpen = (userData: SeminarData | undefined) => {
    setRowDetails(userData)
    setView(true)
  }

  const handleSavedClose = () => {
    setSaved(false);
  };

  const handleEditedClose = () => {
    setEdited(false);
  };

  return <div>
    <Breadcrumbs>
      <Link to={"/"}>
        Home
      </Link>
      <Typography color="text.primary">Drop-ins</Typography>
    </Breadcrumbs>
    <Stack sx={{mt: 4, gap: 1, alignItems: 'stretch'}}>
      <Stack direction={'row'} spacing={2} justifyContent={'space-between'}>
        <Button variant={'outlined'}><Link to={'/drop-ins/new'}>Add new drop-in session</Link></Button>
        <FormControlLabel
          label={'Show all'}
          control={<Checkbox checked={showAll} onChange={() => setShowAll(!showAll)} color="primary"/>}
        />
      </Stack>
      <Snackbar
        anchorOrigin={{vertical: 'top', horizontal: 'center'}}
        open={saved}
        autoHideDuration={6000}
        onClose={() => handleSavedClose()}>
        <Alert onClose={() => handleSavedClose()} severity="success" sx={{width: '100%'}}>
          Drop-In saved successfully ! Please check the drop-in list.
        </Alert>
      </Snackbar>
      <Snackbar
        anchorOrigin={{vertical: 'top', horizontal: 'center'}}
        open={edited}
        autoHideDuration={6000}
        onClose={() => handleEditedClose()}>
        <Alert onClose={() => handleEditedClose()} severity="success" sx={{width: '100%'}}>
          Drop-In updated successfully ! Please check the drop-in list.
        </Alert>
      </Snackbar>
      <Snackbar
        anchorOrigin={{vertical: 'top', horizontal: 'center'}}
        open={error}
        autoHideDuration={6000}
        onClose={() => handleBack}>
        <Alert onClose={handleBack} severity="success" sx={{width: '100%'}}>
          There was a problem fetching the list of drop-ins. Please try reloading the page.
        </Alert>
      </Snackbar>
      <Box sx={{height: 400, width: '100%'}}>
        <DataGrid
          getRowId={(rowsData) => rowsData.id}
          rows={rowsData}
          columns={dropInColumns}
          pageSizeOptions={[5]}
        />
      </Box>
      <ViewDropIn id={rowDetails?.id} title={rowDetails?.title}
                  startTime={rowDetails?.startTime}
                  endTime={rowDetails?.endTime}
                  meetingLink={rowDetails?.meetingLink}
                  view={view} setView={setView}
      />
      <DialogActionComponent actionMethod='DELETE'
                             actionString='remove drop-in'
                             actionUrl={'/drop-in/delete/' + rowDetails?.id}
                             showDialog={showDelete}
                             setShowDialog={setShowDelete}
      />
      <DialogActionComponent actionUrl={'/drop-in/published/' + rowDetails?.id}
                             actionString="publish"
                             actionMethod="POST"
                             showDialog={publish}
                             setShowDialog={setPublish}
      />
      <DialogActionComponent actionUrl={'/drop-in/published/' + rowDetails?.id}
                             actionString="unPublish"
                             actionMethod="POST"
                             showDialog={unPublish}
                             setShowDialog={setUnPublish}
      />
    </Stack>
  </div>
}
