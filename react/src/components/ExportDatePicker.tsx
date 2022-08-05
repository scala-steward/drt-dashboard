import * as React from 'react';
import TextField from '@mui/material/TextField';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider';
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import Button from '@mui/material/Button';
import axios from "axios";
import format from 'date-fns/format';
import fileDownload from 'js-file-download';
import CircularProgress from '@mui/material/CircularProgress';

interface IProps {
    region: string;
}

export default function ExportDatePicker(props: IProps) {
    const [fromValue, setFromValue] = React.useState<Date | null>(null);
    const [toValue, setToValue] = React.useState<Date | null>(null);
    const [progress, setProgress] = React.useState<boolean | false>(false);
    const formattedDate = (date: Date) => format(date as Date, "yyyy-MM-dd")
    const formattedDatetime = (date: Date) => format(date as Date, "yyyyMMddhhmmss")

    const download = () => {
        setProgress(true)
        axios.get("/export/" + props.region + "/" + formattedDate(fromValue) + "/" + formattedDate(toValue), {
            responseType: 'blob',
        }).then(res => {
            setProgress(false)
            let fileName = props.region + "-" + formattedDatetime(new Date()) + "-" + formattedDate(fromValue) + "-to-" + formattedDate(toValue) + ".csv"
            console.log('fileName: ' + fileName);
            fileDownload(res.data, fileName);
        });
    }

    const determineDisplay = () => {
        if (progress) {
            return <div align="center">
                <CircularProgress/>
            </div>
        } else {
            return <div align="center">
                <Button variant="contained" onClick={download}>Download</Button>
            </div>
        }
    };

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
            <span>&nbsp;&nbsp;</span>
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
            <br/>
            {determineDisplay()}
        </LocalizationProvider>
    );
}