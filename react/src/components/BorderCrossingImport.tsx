import React from 'react'
import {Box, FormControl} from "@mui/material"
import Button from "@mui/material/Button"
import CloudUploadIcon from '@mui/icons-material/CloudUpload'
import {styled} from "@mui/material/styles";
import axios from "axios";
import ApiClient from "../services/ApiClient";
import {Helmet} from "react-helmet";
import {adminPageTitleSuffix} from "../utils/common";
import PageContentWrapper from "./PageContentWrapper";

const BorderCrossingImport = () => {
  const [uploading, setUploading] = React.useState(false)
  const [finished, setFinished] = React.useState(false)
  const [insertCount, setInsertCount] = React.useState(0)

  const uploadFile = (file: File) => {
    setUploading(true)
    setFinished(false)
    setInsertCount(0)

    console.log('Uploading file', file)

    const form = new FormData();
    form.append('excel', file);

    axios.post(ApiClient.borderCrossingEndpoint, form)
      .then(response => {
        const insertCount = response.data.inserted as number
        setUploading(false)
        setFinished(true)
        setInsertCount(insertCount)
      })
      .catch((error) => {
        console.error('Error uploading file', error)
        setUploading(false)
      })
  }

  return (
    <PageContentWrapper>
      <Helmet>
        <title>Border-crossing import {adminPageTitleSuffix}</title>
      </Helmet>
      <Box>
        <h1>Border Crossing Import</h1>
        <p>Upload an excel file here. The spreadsheet must contain a sheet named 'Data Response', and be in an expected
          format</p>
        <FormControl>
          <Button
            component="label"
            role={undefined}
            variant="contained"
            tabIndex={-1}
            startIcon={<CloudUploadIcon/>}
            disabled={uploading}
          >
            Upload file
            <VisuallyHiddenInput
              type="file"
              onChange={event => {
                event.target.files && uploadFile(event.target.files[0])
                event.target.value = ''
              }}
            />
          </Button>
          {uploading && <p>Uploading & processing data. Please be patient as this may take a minute.</p>}
          {finished && <p>File uploaded. {insertCount} records found</p>}
        </FormControl>
      </Box>
    </PageContentWrapper>
  )
}

const VisuallyHiddenInput = styled('input')({
  clip: 'rect(0 0 0 0)',
  clipPath: 'inset(50%)',
  height: 1,
  overflow: 'hidden',
  position: 'absolute',
  bottom: 0,
  left: 0,
  whiteSpace: 'nowrap',
  width: 1,
})

export default BorderCrossingImport
