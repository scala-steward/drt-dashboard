import * as React from 'react';
import TextField from '@mui/material/TextField';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider';
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import Button from '@mui/material/Button';
import format from 'date-fns/format';
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import {Stack} from "@mui/material";
import axios from "axios";

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
  const [fromValue, setFromValue] = React.useState<Date | null>(null);
  const [toValue, setToValue] = React.useState<Date | null>(null);

  const formattedDate = (date: Date) => format(date as Date, "yyyy-MM-dd")

  const requestExport = () => {
    fromValue && toValue && axios.post(
      '/export',
      {
        region: props.region,
        startDate: formattedDate(fromValue),
        endDate: formattedDate(toValue),
      } as RegionExportRequest,
    )
    props.handleClose()
  }

  return (
    <LocalizationProvider dateAdapter={AdapterDateFns}>
      <Stack spacing={2} sx={{marginTop: 2}}>
        <DatePicker
          label="From Date"
          value={fromValue}
          onChange={(newValue) => {
            setFromValue(newValue);
          }}
          renderInput={(params) => <TextField {...params} />}
        />
        <DatePicker
          label="To Date"
          value={toValue}
          onChange={(newValue) => {
            setToValue(newValue);
          }}
          renderInput={(params) => <TextField {...params} />}
        />
        <Button startIcon={<FileDownloadIcon/>}
                target="_blank"
                disabled={!fromValue || !toValue}
                onClick={requestExport}
        >
          Request export
        </Button>
      </Stack>
    </LocalizationProvider>
  );
}
