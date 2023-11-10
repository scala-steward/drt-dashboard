import React, {useState} from "react";
import {DataGrid, GridColDef, GridRenderCellParams} from "@mui/x-data-grid";
import IconButton from "@mui/material/IconButton";
import PublishIcon from "@mui/icons-material/Publish";
import UnpublishedIcon from "@mui/icons-material/Unpublished";
import PreviewIcon from "@mui/icons-material/Preview";
import DeleteIcon from "@mui/icons-material/Delete";
import EditIcon from "@mui/icons-material/Edit";
import axios from "axios";
import {Breadcrumbs, FormControlLabel, Stack} from "@mui/material";
import Box from "@mui/material/Box";
import {ViewDropInSession} from "./ViewDropInSession";
import {CalendarViewMonth} from "@mui/icons-material";
import moment from 'moment-timezone';
import {Link} from "react-router-dom";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import {enqueueSnackbar} from "notistack";
import Loading from "../Loading";
import {useDropInSessions} from "../../store/dropInSessions";
import {DialogComponent} from "../DialogComponent";
import ApiClient from "../../services/ApiClient";

moment.locale('en-gb')

export interface SeminarData {
  id: string;
  title: string;
  startTime: string;
  endTime: string;
  meetingLink: string;
  isPublished: boolean;
}

export function DropInSessionsList() {

  const dropInColumns: GridColDef[] = [
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
        return <div>{moment(params.value).format("HH:mm, Do MMM YYYY")}</div>;
      }
    },
    {
      field: 'endTime',
      headerName: 'End Time',
      width: 150,
      renderCell: (params) => {
        return <div>{moment(params.value).format("HH:mm, Do MMM YYYY")}</div>;
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
        return <div>{moment(params.value).format("HH:mm, Do MMM YYYY")}</div>;
      }
    },
    {
      field: 'isPublished',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="publish"
                    onClick={() => params.row.isPublished ? handleUnPublish(params.row) : handlePublish(params.row)}>
          {params.row.isPublished === true ?
            <PublishIcon/> :
            <UnpublishedIcon/>}
        </IconButton>
      ),
    },
    {
      field: 'view',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="view" onClick={() => rowClickOpen(params.row as SeminarData)}>
          <PreviewIcon/>
        </IconButton>
      ),
    },
    {
      field: 'delete',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="delete" onClick={() => handleDelete(params.row as SeminarData)}>
          <DeleteIcon/>
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
        <Link to={`/drop-ins/list/registered-users/${params.row.id}`}>
          <IconButton aria-label="users-registered">
            <CalendarViewMonth/>
          </IconButton>
        </Link>
      ),
    },
  ];

  const [requestedAt, setRequestedAt] = useState(moment().valueOf())
  const [showAll, setShowAll] = useState(false)
  const {dropInSessions, loading, failed} = useDropInSessions(showAll, requestedAt)
  const [showDelete, setShowDelete] = useState(false)
  const [publish, setPublish] = useState(false)
  const [unPublish, setUnPublish] = useState(false)
  const [view, setView] = useState(false)
  const [rowDetails, setRowDetails] = React.useState({} as SeminarData | undefined)

  const handlePublish = (userData: SeminarData | undefined) => {
    setRowDetails(userData)
    setPublish(true)
  }

  const handleUnPublish = (userData: SeminarData | undefined) => {
    setRowDetails(userData)
    setUnPublish(true);
  }

  const handleDelete = (userData: SeminarData | undefined) => {
    setRowDetails(userData)
    setShowDelete(true);
  }

  const rowClickOpen = (userData: SeminarData | undefined) => {
    setRowDetails(userData)
    setView(true)
  }

  const updatePublished = (id: string, isPublished: boolean) => {
    axios.post(`${ApiClient.dropInSessionUpdatePublishedEndpoint}/${id}`, {published: isPublished})
      .then(response => {
        if (response.status === 200) {
          enqueueSnackbar('Drop-in session updated')
          setRequestedAt(moment().valueOf())
        } else
          enqueueSnackbar('There was a problem updating the drop-in session. Please try again later.')
      })
  }

  const deleteSession = (id: string) => {
    axios.delete(`${ApiClient.dropInSessionDeletePublishedEndpoint}/${id}`)
      .then(response => {
        if (response.status === 200) {
          enqueueSnackbar('Drop-in session deleted')
          setRequestedAt(moment().valueOf())
        } else
          enqueueSnackbar('There was a problem deleting the drop-in session. Please try again later.')
      })
  }

  return <Stack gap={4} alignItems={'stretch'} sx={{mt: 2}}>
    <Breadcrumbs>
      <Link to={"/"}>
        Home
      </Link>
      <Typography color="text.primary">Drop-in sessions</Typography>
    </Breadcrumbs>
    <Stack direction={'row'} justifyContent={'space-between'}>
      <Link to={'/drop-ins/edit'}><Button variant={'outlined'}>New drop in session</Button></Link>
      <FormControlLabel
        label={'Show all'}
        control={<Checkbox checked={showAll} onChange={() => setShowAll(!showAll)} color="primary"/>}
      />
    </Stack>
    {loading ?
      <Loading/> :
      failed ?
        <Typography variant={'body1'}>Sorry, I couldn't load the existing pauses</Typography> :
        dropInSessions.length === 0 ?
          <Typography variant={'body1'}>There are no current or upcoming pauses</Typography> :
          <Box sx={{height: 400, width: '100%'}}>
            <DataGrid
              getRowId={(rowsData) => rowsData.id}
              rows={dropInSessions}
              columns={dropInColumns}
              pageSizeOptions={[5]}
            />
          </Box>
    }
    <ViewDropInSession id={rowDetails?.id}
                       title={rowDetails?.title}
                       startTime={rowDetails?.startTime}
                       endTime={rowDetails?.endTime}
                       meetingLink={rowDetails?.meetingLink}
                       view={view}
                       setView={setView}/>
    {showDelete && <DialogComponent
        actionText='remove drop-in'
        onCancel={() => setShowDelete(false)}
        onConfirm={() => {
          deleteSession(rowDetails?.id as string)
          setShowDelete(false)
        }}/>}
    {publish && <DialogComponent
        actionText="publish"
        onCancel={() => setShowDelete(false)}
        onConfirm={() => {
          updatePublished(rowDetails?.id as string, true)
          setPublish(false)
        }}/>}
    {unPublish && <DialogComponent
        actionText="unPublish"
        onCancel={() => setShowDelete(false)}
        onConfirm={() => {
          updatePublished(rowDetails?.id as string, false)
          setUnPublish(false)
        }}/>}
  </Stack>
}
