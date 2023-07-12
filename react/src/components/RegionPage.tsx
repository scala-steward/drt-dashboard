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

interface IProps {
  user: UserProfile;
  config: ConfigValues;
  region: string;
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

  const fetchDownloads = () => {
    axios
      .get(`/export/${props.region}`)
      .then((response) => {
        setDownloads(response.data as Download[])
        setTimeout(() => {
          fetchDownloads()
        }, 5000)
      })
  }

  useEffect(() => {
    fetchDownloads()
    // setDownloads([
    //   {
    //     id: "1",
    //     startDate: new Date(2022, 7, 1),
    //     endDate: new Date(2023, 7, 1),
    //     created: new Date(2023, 7, 10, 12, 10, 0),
    //     status: "Pending"
    //   },
    //   {
    //     id: "2",
    //     startDate: new Date(2022, 7, 1),
    //     endDate: new Date(2023, 7, 1),
    //     created: new Date(2023, 7, 9, 15, 35, 0),
    //     status: "Complete"
    //   },
    // ])
  }, [setDownloads])

  function formatDateDDMMYYYYHHmm(date: Date) {
    return moment(date).format("DD/MM/YYYY HH:mm")
  }

  function formatDateDDMMYYYY(date: Date) {
    return moment(date).format("DD/MM/YYYY")
  }

  function downloadExport(download: Download) {
    const url = `/export/North/${download.createdAt}`

    window.showSaveFilePicker().then(handle => {
      handle.createWritable().then(writableStream => {
        fetch(url, {
          method: 'GET',
        }).then(res => {
          const reader = res.body.getReader();

          reader.read().then(function processText({done, value}) {
            // Result objects contain two properties:
            // done  - true if the stream has already given you all its data.
            // value - some data. Always undefined when done is true.
            if (done) {
              console.log("Stream complete");
              writableStream.close()
              return;
            }

            const chunk = value;

            writableStream.write(chunk).then(() => {
              // Read some more, and call this function again
              return reader.read().then(processText);
            })
          });
        })
      })
    })
  }

  return <div className="flex-container">
    <Breadcrumbs aria-label="breadcrumb">
      <Link underline="hover" color="inherit" href="/">DRT</Link>
      <Typography color="text.primary">{props.region}</Typography>
    </Breadcrumbs>
    {props.user.roles.includes("rcc:" + props.region.toLowerCase()) ?
      <Box>
        <h1>{props.region} region</h1>
        <p>You can download an arrivals export covering all port terminals in
          this region.</p>
        <ArrivalExport region={props.region}/>
        <h2>Downloads</h2>
        {downloads ?
          <Grid container spacing={2}>
            <Grid xs={3}><Typography fontWeight="bold">Created</Typography></Grid>
            <Grid xs={6}><Typography fontWeight="bold">Date range</Typography></Grid>
            <Grid xs={3}></Grid>
            {downloads.map(download => {
              const downloadUrl = `/export/${download.region}/${download.createdAt}`
              return <>
                <Grid xs={3}><Typography>{formatDateDDMMYYYYHHmm(new Date(download.createdAt))}</Typography></Grid>
                <Grid xs={6}>
                  <Typography>{formatDateDDMMYYYY(download.startDate)} - {formatDateDDMMYYYY(download.endDate)}</Typography>
                </Grid>
                <Grid xs={3}><Typography>
                  {download.status === 'complete' ?
                    <Link
                      onClick={
                        (e) => {
                          downloadExport(download)
                          e.preventDefault()
                        }}
                      href={downloadUrl} target={'_blank'}>Download</Link> :
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
