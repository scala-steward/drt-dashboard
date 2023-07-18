import React, {useEffect, useState} from "react";
import axios, {AxiosResponse} from "axios";
import {DataGrid, GridColDef, GridRenderCellParams, GridRowModel} from "@mui/x-data-grid";
import Box from "@mui/material/Box";
import {Button} from "@mui/material";
import {PreviewComponent} from "./PreviewComponent";
import {FeatureGuideEdit} from "./FeatureGuideEdit";
import PublishIcon from '@mui/icons-material/Publish';
import UnpublishedIcon from '@mui/icons-material/Unpublished';
import IconButton from "@mui/material/IconButton";
import PreviewIcon from '@mui/icons-material/Preview';
import DeleteIcon from "@mui/icons-material/Delete";
import {DeleteComponent} from "./DeleteComponent";
import EditIcon from '@mui/icons-material/Edit';
import {PublishComponent} from "./PublishComponent";

interface Props {
  setViewFeatureGuides: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export interface FeatureData {
  id: string;
  title: string;
  fileName: string;
  uploadTime: string;
  markdownContent: string;
}

export const ListFeatureGuide: React.FC = (props: Props) => {

  const featureColumns: GridColDef[] = [
    {
      field: 'id',
      headerName: 'Id',
      width: 50
    },
    {
      field: 'title',
      headerName: 'Title',
      width: 200,
    },
    {
      field: 'fileName',
      headerName: 'File Name',
      width: 200,
    },
    {
      field: 'uploadTime',
      headerName: 'Upload Time',
      description: 'This column has a value getter and is not sortable.',
      sortable: false,
      width: 200,
    },
    {
      field: 'markdownContent',
      headerName: 'Markdown Content',
      width: 200,
    },
    {
      field: 'published',
      headerName: 'Published',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="publish">
          {params.row.published === true ?
            <PublishIcon onClick={() => handleUnPublish(params.row as FeatureData)}/> :
            <UnpublishedIcon onClick={() => handlePublish(params.row as FeatureData)}/>}
        </IconButton>
      ),
    },
    {
      field: 'preview',
      headerName: 'Preview',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="preview">
          <PreviewIcon onClick={() => rowClickOpen(params.row as FeatureData)}/>
        </IconButton>
      ),
    },
    {
      field: 'delete',
      headerName: 'Delete',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="delete">
          <DeleteIcon onClick={() => handleDelete(params.row as FeatureData)}/>
        </IconButton>
      ),
    },
    {
      field: 'edit',
      headerName: 'Edit',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="delete">
          <EditIcon onClick={() => handleEdit(params.row as FeatureData)}/>
        </IconButton>
      ),
    },
  ];

  const [rowsData, setRowsData] = React.useState([] as GridRowModel[]);
  const [receivedData, setReceivedData] = React.useState(false);
  const [openPreview, setOpenPreview] = React.useState(false)
  const [rowDetails, setRowDetails] = React.useState({} as FeatureData | undefined)
  const [error, setError] = useState(false);
  const [showEdit, setShowEdit] = React.useState(false)
  const [showDelete, setShowDelete] = useState(false);
  const [publish, setPublish] = useState(false);
  const [unPublish, setUnPublish] = useState(false);

  const handlePublish = (userData: FeatureData | undefined) => {
    setRowDetails(userData)
    setPublish(true);
  }

  const handleUnPublish = (userData: FeatureData | undefined) => {
    setRowDetails(userData)
    setUnPublish(true);
  }


  const handleEdit = (userData: FeatureData | undefined) => {
    setRowDetails(userData)
    setShowEdit(true);
    setOpenPreview(false)
  }

  const handleDelete = (userData: FeatureData | undefined) => {
    setRowDetails(userData)
    setShowDelete(true);
  }

  const handleResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      setRowsData(response.data)
      setReceivedData(true);
      props.setViewFeatureGuides(true)
    } else {
      setError(true);
      response.data
    }
  }

  useEffect(() => {
    if (!receivedData) {
      axios.get('/guide/getFeatureGuides')
        .then(response => handleResponse(response))
        .then(data => {
          console.log(data);
        }).catch(error => {
        setError(true);
        console.error(error);
      });
    }
  }, [receivedData]);

  const handleBack = () => {
    setError(false);
    props.setViewFeatureGuides(false)
    setReceivedData(false)
  }

  const rowClickOpen = (userData: FeatureData | undefined) => {
    setRowDetails(userData)
    setOpenPreview(true)
  }

  return (
    error ? <div style={{marginTop: '20px', color: 'red'}}> Errored for the task <br/>
        <Button style={{float: 'right'}} variant="outlined" color="primary" onClick={handleBack}>back</Button>
      </div> :
      publish ? <PublishComponent id={rowDetails?.id} showAction={publish} setShowAction={setPublish}
                                  setReceivedData={setReceivedData} actionString={"publish"}/> :
        unPublish ? <PublishComponent id={rowDetails?.id} showAction={unPublish} setShowAction={setUnPublish}
                                      setReceivedData={setReceivedData} actionString={"unPublish"}/> :
          showDelete ?
            <DeleteComponent id={rowDetails?.id} showDelete={showDelete} setShowDelete={setShowDelete}
                             setOpenPreview={setOpenPreview} setReceivedData={setReceivedData}/> :
            showEdit ? <FeatureGuideEdit id={rowDetails?.id} title={rowDetails?.title}
                                         videoURL={"/guide/get-feature-videos/" + rowDetails?.fileName}
                                         markdownContent={rowDetails?.markdownContent}
                                         showEdit={showEdit} setShowEdit={setShowEdit}
                                         setReceivedData={setReceivedData}
              /> :
              <div>
                <h1>Feature Guide List</h1>
                <Box sx={{height: 400, width: '100%'}}>
                  <DataGrid
                    getRowId={(rowsData) => rowsData.id}
                    rows={rowsData}
                    columns={featureColumns}
                    pageSize={5}
                    rowsPerPageOptions={[5]}
                    experimentalFeatures={{newEditingApi: true}}
                  />
                  <Button style={{float: 'right'}} variant="outlined"
                          color="primary"
                          onClick={handleBack}>back</Button>
                </Box>
                <PreviewComponent id={rowDetails?.id} title={rowDetails?.title}
                                  videoURL={"/guide/get-feature-videos/" + rowDetails?.fileName}
                                  markdownContent={rowDetails?.markdownContent}
                                  openPreview={openPreview} setOpenPreview={setOpenPreview}
                                  setReceivedData={setReceivedData} isEdit={true}
                                  showEdit={showEdit} setShowEdit={setShowEdit}/>
              </div>
  )
}


export default ListFeatureGuide;
