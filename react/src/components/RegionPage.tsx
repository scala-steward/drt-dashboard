import * as React from 'react';
import {useEffect} from 'react';
import ArrivalExport from './ArrivalExport';
import {UserProfile} from "../model/User";
import {ConfigValues} from "../model/Config";
import Typography from "@mui/material/Typography";
import Link from "@mui/material/Link";
import {Box, Breadcrumbs} from "@mui/material";
import Grid from "@mui/material/Unstable_Grid2";
import moment from "moment-timezone";
import axios from "axios";
import {useParams} from "react-router-dom";
import {StringUtils} from "../utils/StringUtils";
import ApiClient from "../services/ApiClient";

interface IProps {
  user: UserProfile;
  config: ConfigValues;
}

interface Download {
  email: string
  region: string
  startDate: Date
  endDate: Date
  createdAt: number
  status: string
}

export const RegionPage = (props: IProps) => {

  const [downloads, setDownloads] = React.useState<Download[] | undefined>(undefined)

  const { regionName } = useParams() as { regionName: string }

  const fetchDownloads = () => {
    axios
      .get(`${ApiClient.exportRegionEndpoint}/${regionName}`)
      .then((response) => {
        setDownloads(response.data as Download[])
        setTimeout(() => {
          fetchDownloads()
        }, 5000)
      })
      .catch((error) => {
        console.log(error)
        setTimeout(() => {
          fetchDownloads()
        }, 5000)
      })
  }

  useEffect(() => {
    fetchDownloads()
  }, [setDownloads])

  function formatDateDDMMYYYYHHmm(date: Date) {
    return moment(date).format("DD/MM/YYYY HH:mm")
  }

  function formatDateDDMMYYYY(date: Date) {
    return moment(date).format("DD/MM/YYYY")
  }

  const sortedDownloads  = downloads ? downloads.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1)) : undefined

  return <div className="flex-container">
    <Breadcrumbs aria-label="breadcrumb">
      <Link underline="hover" color="inherit" href="/">DRT</Link>
      <Typography color="text.primary">{StringUtils.ucFirst(regionName)}</Typography>
    </Breadcrumbs>
    {props.user.roles.includes("rcc:" + regionName.toLowerCase()) ?
      <Box>
        <h1>{StringUtils.ucFirst(regionName)} region</h1>
        <p>You can download an arrivals export covering all port terminals in
          this region.</p>
        <ArrivalExport region={regionName}/>
        <h2>Downloads</h2>
        {sortedDownloads ?
          <Grid container spacing={2}>
            <Grid xs={3}><Typography fontWeight="bold">Created</Typography></Grid>
            <Grid xs={6}><Typography fontWeight="bold">Date range</Typography></Grid>
            <Grid xs={3}></Grid>
            {sortedDownloads.map(download => {
              const downloadUrl = `${ApiClient.exportRegionEndpoint}/${download.region}/${download.createdAt}`
              return <>
                <Grid xs={3}><Typography>{formatDateDDMMYYYYHHmm(new Date(download.createdAt))}</Typography></Grid>
                <Grid xs={6}>
                  <Typography>{formatDateDDMMYYYY(download.startDate)} - {formatDateDDMMYYYY(download.endDate)}</Typography>
                </Grid>
                <Grid xs={3}><Typography>
                  {download.status === 'complete' ?
                    <Link href={downloadUrl} target={'_blank'}>Download</Link> :
                    download.status
                  }
                </Typography></Grid>
              </>
            })}
          </Grid> :
          <p>No downloads yet.</p>
        }
      </Box> :
      <Box>
        <p>You don't have access to this page. To request access please get in touch with us at <a
          href={"mailto:" + props.config.teamEmail}> {props.config.teamEmail}</a>.</p>
      </Box>
    }
  </div>
}
