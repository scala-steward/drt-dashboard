import * as React from 'react';
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider';
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import Button from '@mui/material/Button';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import {Stack} from "@mui/material";
import axios from "axios";
import {AdapterMoment} from "@mui/x-date-pickers/AdapterMoment";
import {Moment} from "moment";

interface IProps {
  region: string;
  handleClose: () => void;
}

interface RegionExportRequest {
  region: string
  startDate: string
  endDate: string
}

export default function ExportDatePicker(props: IProps) {
  const [fromValue, setFromValue] = React.useState<Moment | null>(null);
  const [toValue, setToValue] = React.useState<Moment | null>(null);

  const formattedDate = (date: Moment) => date.format("DD-MM-yyyy")

  const requestExport = () => {
    fromValue && toValue && axios.post(
      '/export-region',
      {
        region: props.region,
        startDate: formattedDate(fromValue),
        endDate: formattedDate(toValue),
      } as RegionExportRequest,
    )
    props.handleClose()
  }

  return <LocalizationProvider dateAdapter={AdapterMoment} adapterLocale={'en-gb'}>
    <Stack spacing={2} sx={{mt: 2}}>
      <DatePicker
        label="From Date"
        value={fromValue}
        onChange={(newValue) => setFromValue(newValue)}
        slotProps={{textField: {variant: 'outlined'}}}
      />
      <DatePicker
        label="To Date"
        value={toValue}
        onChange={(newValue) => setToValue(newValue)}
        slotProps={{textField: {variant: 'outlined'}}}
      />
      <Button startIcon={<FileDownloadIcon/>}
              disabled={!fromValue || !toValue}
              onClick={requestExport}
      >
        Request export
      </Button>
    </Stack>
  </LocalizationProvider>
}
