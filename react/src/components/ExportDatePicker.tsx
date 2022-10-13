import * as React from 'react';
import TextField from '@mui/material/TextField';
import {AdapterDateFns} from '@mui/x-date-pickers/AdapterDateFns';
import {LocalizationProvider} from '@mui/x-date-pickers/LocalizationProvider';
import {DatePicker} from '@mui/x-date-pickers/DatePicker';
import Button from '@mui/material/Button';
import format from 'date-fns/format';
import FileDownloadIcon from '@mui/icons-material/FileDownload';

interface IProps {
    region: string;
}

export default function ExportDatePicker(props: IProps) {
    const [fromValue, setFromValue] = React.useState<Date | null>(null);
    const [toValue, setToValue] = React.useState<Date | null>(null);
    const formattedDate = (date: Date) => format(date as Date, "yyyy-MM-dd")

    const renderDownloadButton = () => {
        if (fromValue && toValue) {
            return <div align="center">
                <Button startIcon={<FileDownloadIcon/>} target="_blank"
                        href={"/export/" + props.region + "/" + formattedDate(fromValue) + "/" + formattedDate(toValue)}>Download</Button>
            </div>
        }
    };

    return (
        <LocalizationProvider dateAdapter={AdapterDateFns}>
            <div className="flex-container">
                <div>
                    <DatePicker
                        label="From Date"
                        value={fromValue}
                        onChange={(newValue) => {
                            setFromValue(newValue);
                        }}
                        renderInput={(params) => <TextField {...params} />}
                    />
                </div>
                <div>
                    <DatePicker
                        label="To Date"
                        value={toValue}
                        onChange={(newValue) => {
                            setToValue(newValue);
                        }}
                        renderInput={(params) => <TextField {...params} />}
                    />
                </div>
            </div>
            {renderDownloadButton()}
        </LocalizationProvider>
    );
}