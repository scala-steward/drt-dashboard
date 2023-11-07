import * as React from 'react';
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
      '/export-region',
      {
        region: props.region,
        startDate: formattedDate(fromValue),
        endDate: formattedDate(toValue),
      } as RegionExportRequest,
    )
    props.handleClose()
  }

  return <LocalizationProvider dateAdapter={AdapterDateFns}>
    <Stack spacing={2} sx={{marginTop: 2}}>
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
