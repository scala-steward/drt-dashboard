import React, {useState} from "react";
import {DataGrid, GridColDef} from "@mui/x-data-grid";
import moment from "moment-timezone";
import {useUserFeedbacks} from "../../store/feedbacks";
import Loading from "../Loading";
import { Typography, Button, Box, Breadcrumbs, Stack, Link as MuiLink } from "@mui/material";
import { Link } from "react-router-dom";
import FileDownloadIcon from "@mui/icons-material/FileDownload";
import ApiClient from "../../services/ApiClient";
import {adminPageTitleSuffix} from "../../utils/common";
import {Helmet} from "react-helmet";
import PageContentWrapper from '../PageContentWrapper';

export function FeedbackList() {
  const [requestedAt, setRequestedAt] = useState(moment().valueOf())

  const {userFeedbacks, loading, failed} = useUserFeedbacks(requestedAt)

  const abFeatureColumns: GridColDef[] = [
    {
      field: 'email',
      headerName: 'Email',
      width: 200,
    },
    {
      field: 'createdAt',
      headerName: 'Created at',
      width: 150,
      renderCell: (params) => {
        return <div>{moment(params.value).format("HH:mm, Do MMM YYYY")}</div>;
      }
    },
    {
      field: 'feedbackType',
      headerName: 'Feedback',
      description: 'Feedback type',
      width: 80,
    },
    {
      field: 'bfRole',
      headerName: 'BF role',
      width: 150
    },
    {
      field: 'drtQuality',
      headerName: 'Quality',
      width: 100,
    },
    {
      field: 'drtLikes',
      headerName: 'Likes',
      width: 150,
    },
    {
      field: 'drtImprovements',
      headerName: 'Improvements',
      width: 150
    },
    {
      field: 'participationInterest',
      headerName: 'Interest',
      width: 60,
    },
    {
      field: 'abVersion',
      headerName: 'AB Version',
      width: 80,
    },
  ];


  return <PageContentWrapper>
    <Helmet>
      <title>Feedback {adminPageTitleSuffix}</title>
    </Helmet>
    <Stack gap={4} alignItems={'stretch'} sx={{mt: 2}}>
      <Breadcrumbs>
        <Link to={"/"}>
          Home
        </Link>
        <Typography color="text.primary">User feedback responses</Typography>
      </Breadcrumbs>
      <Stack direction={'row'} justifyContent={'space-between'}>
        <Button
          sx={{maxWidth: '350px'}}
          startIcon={<FileDownloadIcon/>}
          component={MuiLink}
          href={`${ApiClient.feedBacksEndpoint}/export`}
          target="_blank"
          onClick={() => setRequestedAt(moment().valueOf())}
        > Download feedback responses</Button>
      </Stack>
      {loading ? <Loading/> :
        failed ?
          <Typography variant={'body1'}>Sorry, I couldn't load the existing pauses</Typography> :
          userFeedbacks.length === 0 ?
            <Typography variant={'body1'}>There are no current or upcoming pauses</Typography> :
            <Box sx={{height: 400, width: '100%'}}>
              <DataGrid
                getRowId={(rowsData) => rowsData.email + '_' + rowsData.createdAt}
                rows={userFeedbacks}
                columns={abFeatureColumns}
                pageSizeOptions={[5]}
              />
            </Box>
      }
    </Stack>
  </PageContentWrapper>
}
