import React from "react";
import Button from "@mui/material/Button";
import FileDownloadIcon from "@mui/icons-material/FileDownload";
import ApiClient from "../services/ApiClient";

export function ExportConfig() {
  return <div>
    <h1>Export Config</h1>
    <Button
      sx={{maxWidth: '350px'}}
      startIcon={<FileDownloadIcon/>}
      href={`${ApiClient.exportConfigEndpoint}`}
      target="_blank"
    > Download ports config</Button>
  </div>
}
