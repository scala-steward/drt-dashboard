import * as React from 'react';
import TextField from '@mui/material/TextField';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import Button from '@mui/material/Button';
import axios from "axios";
import format from 'date-fns/format';
import fileDownload from 'js-file-download';

interface IProps {
    region: string;
}

export default function ExportDatePicker(props: IProps) {
    const [fromValue, setFromValue] = React.useState<Date | null>(null);
    const [toValue, setToValue] = React.useState<Date | null>(null);

    const formattedDate = (date: Date) => format(date as Date, "yyyy-MM-dd")

    const download = () => {
        axios.get("/export/" + props.region + "/" + formattedDate(fromValue) + "/" + formattedDate(toValue), {
            responseType: 'blob',
        }).then(res => {
            let fileName = props.region + "-" + formattedDate(fromValue) + "-" + formattedDate(toValue) + ".csv"
            console.log('fileName: ' + fileName);
            fileDownload(res.data, fileName);
        });
    }


    return (
        <LocalizationProvider dateAdapter={AdapterDateFns}>
            <DatePicker
                label="From Date"
                value={fromValue}
                onChange={(newValue) => {
                    setFromValue(newValue);
                }}
                renderInput={(params) => <TextField {...params} />}
            />
            <span> - </span>
            <DatePicker
                label="To Date"
                value={toValue}
                onChange={(newValue) => {
                    setToValue(newValue);
                }}
                renderInput={(params) => <TextField {...params} />}
            />
            <br/>
            <br/>
            <div style={{float: 'centre'}}>
                <Button variant="contained"  onClick={download}>Download</Button>
            </div>
        </LocalizationProvider>
    );
}
