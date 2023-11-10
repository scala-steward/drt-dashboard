import React, {useState} from "react"
import {DataGrid, GridColDef, GridRenderCellParams} from "@mui/x-data-grid"
import Box from "@mui/material/Box"
import {Breadcrumbs, Stack} from "@mui/material"
import {PreviewComponent} from "./PreviewComponent"
import PublishIcon from '@mui/icons-material/Publish'
import UnpublishedIcon from '@mui/icons-material/Unpublished'
import IconButton from "@mui/material/IconButton"
import PreviewIcon from '@mui/icons-material/Preview'
import DeleteIcon from "@mui/icons-material/Delete"
import EditIcon from '@mui/icons-material/Edit'
import {Link, useNavigate} from "react-router-dom"
import Typography from "@mui/material/Typography"
import {DialogComponent} from "../DialogComponent"
import {deleteFeatureGuide, updatePublishedStatus, useFeatureGuides} from "../../store/featureGuides"
import Loading from "../Loading"
import moment from "moment-timezone"

export interface FeatureData {
  id: string
  title: string
  fileName: string
  uploadTime: string
  markdownContent: string
}

export const FeatureGuidesList = () => {
  const navigate = useNavigate()
  const [previewGuide, setPreviewGuide] = React.useState<FeatureData | undefined>(undefined)

  const featureColumns: GridColDef[] = [
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
      field: 'markdownContent',
      headerName: 'Markdown Content',
      width: 200,
    },
    {
      field: 'uploadTime',
      headerName: 'Created',
      description: 'This column has a value getter and is not sortable.',
      sortable: false,
      width: 200,
    },
    {
      field: 'published',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="publish"
                    onClick={() => params.row.published ? setUnpublishId(params.row.id) : setPublishId(params.row.id)}>
          {params.row.published === true ?
            <PublishIcon/> :
            <UnpublishedIcon/>}
        </IconButton>
      ),
    },
    {
      field: 'preview',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="preview" onClick={() => setPreviewGuide(params.row as FeatureData)}>
          <PreviewIcon/>
        </IconButton>
      ),
    },
    {
      field: 'delete',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="delete" onClick={() => setDeleteId(params.row.id)}>
          <DeleteIcon/>
        </IconButton>
      ),
    },
    {
      field: 'edit',
      headerName: '',
      width: 50,
      renderCell: (params: GridRenderCellParams) => (
        <IconButton aria-label="delete" onClick={() => navigate(`/feature-guides/edit/${params.row.id}`)}>
          <EditIcon/>
        </IconButton>
      ),
    },
  ]

  const [requestedAt, setRequestedAt] = useState<number>(moment().valueOf())
  const {features, loading, failed} = useFeatureGuides(requestedAt)
  const [deleteId, setDeleteId] = useState(undefined)
  const [publishId, setPublishId] = useState(undefined)
  const [unpublishId, setUnpublishId] = useState(undefined)

  return <Stack sx={{mt: 2, gap: 4, alignItems: 'stretch'}}>
    <Breadcrumbs>
      <Link to={"/"}>
        Home
      </Link>
      <Typography color="text.primary">Feature guides</Typography>
    </Breadcrumbs>
    <Stack sx={{mt: 2, gap: 4, alignItems: 'stretch'}}>
      {loading ?
        <Loading/> :
        failed ? <Typography variant={'body1'}>Failed to load features. Try refreshing the page</Typography> :
          <Box sx={{height: 400, width: '100%'}}>
            <DataGrid
              getRowId={(rowsData) => rowsData.id}
              rows={features}
              columns={featureColumns}
              pageSizeOptions={[5]}
            />
          </Box>
      }

      {previewGuide && <PreviewComponent guide={previewGuide}
                                         videoUrl={"/guide/get-feature-videos/" + previewGuide.fileName}
                                         onClose={() => {
                                           setPreviewGuide(undefined)
                                           setRequestedAt(moment().valueOf())
                                         }}
                                         actionsAvailable={true}
      />}
    </Stack>
    {
      publishId && <DialogComponent actionText={'publish'}
                                    onConfirm={() => {
                                      updatePublishedStatus(previewGuide?.id as string, true, () => setPublishId(undefined))
                                      setRequestedAt(moment().valueOf())
                                    }}
                                    onCancel={() => setPublishId(undefined)}/>
    }
    {
      unpublishId && <DialogComponent actionText={'unpublish'}
                                      onConfirm={() => {
                                        updatePublishedStatus(previewGuide?.id as string, true, () => setUnpublishId(undefined))
                                        setRequestedAt(moment().valueOf())
                                      }}
                                      onCancel={() => setUnpublishId(undefined)}/>
    }
    {
      deleteId && <DialogComponent actionText={'delete'}
                                   onConfirm={() => {
                                     deleteFeatureGuide(previewGuide?.id as string)
                                     setRequestedAt(moment().valueOf())
                                   }}
                                   onCancel={() => setDeleteId(undefined)}/>
    }
  </Stack>
}
