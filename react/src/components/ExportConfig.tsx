import React from "react";
import Button from "@mui/material/Button";
import FileDownloadIcon from "@mui/icons-material/FileDownload";
import ApiClient from "../services/ApiClient";
import {Helmet} from "react-helmet";
import {adminPageTitleSuffix} from "../utils/common";
import PageContentWrapper from './PageContentWrapper';

export function ExportConfig() {
  return <PageContentWrapper>
    <Helmet>
      <title>Export Config {adminPageTitleSuffix}</title>
    </Helmet>
    <h1>Export Config</h1>
    <Button
      sx={{maxWidth: '350px'}}
      startIcon={<FileDownloadIcon/>}
      href={`${ApiClient.exportConfigEndpoint}`}
      target="_blank"
    > Download ports config</Button>
  </PageContentWrapper>
}
