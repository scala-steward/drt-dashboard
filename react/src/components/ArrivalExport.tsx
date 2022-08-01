import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import Modal from '@mui/material/Modal';
import ExportDatePicker from "./ExportDatePicker";

const style = {
    position: 'absolute' as 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 500,
    bgcolor: 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
};

interface IProps {
    region: string;
}

export default function ArrivalExport(props: IProps) {
    const [open, setOpen] = React.useState(false);
    const handleOpen = () => setOpen(true);
    const handleClose = () => setOpen(false);

    return (
        <div>
            <Button onClick={handleOpen}>{props.region}</Button>
            <Modal
                open={open}
                onClose={handleClose}
                aria-labelledby="modal-modal-title"
                aria-describedby="modal-modal-description">
                <Box sx={style}>
                    <Typography style={{float: 'centre'}} id="modal-modal-title" variant="h6" component="h2">
                        Arrival Export
                    </Typography>
                    <Typography id="modal-modal-description" sx={{mt: 2}}>
                        Choose dates and click on download to get arrival exports.
                    </Typography>
                    <br/>
                    <ExportDatePicker region={props.region}/>
                    <Button style={{float: 'right'}} onClick={handleClose}>Close</Button>
                </Box>
            </Modal>
        </div>
    );
}
