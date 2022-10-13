import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import Modal from '@mui/material/Modal';
import TextField from '@mui/material/TextField';
import List from '@mui/material/List';
import ListItem from '@mui/material/ListItem';

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
    openModal: boolean;
    setOpenModal: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
    rccOption: boolean;
    rccRegions: string[];
    ports: string[];
    manageStaff: boolean;
    portOrRegionText: string;
    setPortOrRegionText: ((value: (((prevState: string) => string) | string)) => void)
    staffText: string;
    setStaffText: ((value: (((prevState: string) => string) | string)) => void)
    saveCallback: () => void;
}

export default function AccessRequestAdditionalInformationForm(props: IProps) {
    const [open, setOpen] = React.useState(true);
    const handleClose = () => {
        props.setOpenModal(false);
        setOpen(false);
    }

    const handleEvent = () => {
        props.saveCallback();
    };

    const handlePortOrRegionTextChange = (event:any) => {
        props.setPortOrRegionText(event.target.value);
    };

    const handleStaffTextChange = (event:any) => {
        props.setStaffText(event.target.value);
    };

    const rccOptionQuestions = () => {
        return <List sx={{width: '100%', bgcolor: 'background.paper'}}>
            <ListItem alignItems="flex-start">
                {(props.rccRegions.length > 1) ?
                    <Typography align="left" id="modal-modal-title" sx={{mt: 2}}>
                        Please let us know why you need access to more than one region
                        <TextField style={{width: "100%"}}
                                   id="outlined-basic"
                                   label="Enter text"
                                   variant="outlined"
                                   required
                                   value={props.portOrRegionText}
                                   onChange={handlePortOrRegionTextChange}/>
                    </Typography>
                    : <span/>
                }
            </ListItem>
            <ListItem alignItems="flex-start">
                {(props.manageStaff) ?
                    <Typography align="left" id="modal-modal-description" sx={{mt: 2}}>
                        Please let us know why you need to edit staffing
                        <TextField style={{width: "100%"}}
                                   id="outlined-basic"
                                   label="Enter text"
                                   variant="outlined"
                                   required
                                   value={props.staffText}
                                   onChange={handleStaffTextChange}/>
                    </Typography>
                    : <span/>
                }
            </ListItem>
        </List>
    }

    const portOptionQuestions = () => {
        return <List sx={{width: '100%', bgcolor: 'background.paper'}}>
            <ListItem alignItems="flex-start">
                {(props.ports.length > 1) ?
                    <Typography align="left" id="modal-modal-description" sx={{mt: 2}}>
                        Please let us know why you need access to more than one port
                        <TextField style={{width: "100%"}}
                                   id="outlined-basic"
                                   label="Enter text"
                                   variant="outlined"
                                   required
                                   value={props.portOrRegionText}
                                   onChange={handlePortOrRegionTextChange}
                        />
                    </Typography>
                    : <span/>
                }
            </ListItem>
            <ListItem alignItems="flex-start">
                {(props.ports.length > 0 && props.manageStaff) ?
                    <Typography align="left" id="modal-modal-description" sx={{mt: 2}}>
                        Please let us know why you need to edit staffing
                        <TextField style={{width: "100%"}}
                                   id="outlined-basic"
                                   label="Enter text"
                                   variant="outlined"
                                   required
                                   value={props.staffText}
                                   onChange={handleStaffTextChange}/>
                    </Typography>
                    : <span/>
                }
            </ListItem>
        </List>
    }

    const enableSubmitRequest = () => {
        if (((props.rccOption && props.rccRegions.length > 1) || (!props.rccOption && props.ports.length > 1)) && props.manageStaff)
            return props.portOrRegionText.length > 1 && props.staffText.length > 1
        else if (props.manageStaff)
            return props.staffText.length > 1
        else if (props.rccRegions.length > 1 || props.ports.length > 1)
            return props.portOrRegionText.length > 1
    }

    return (
        <div className="flex-container">
            <div>
                <Modal
                    open={open}
                    onClose={handleClose}
                    aria-labelledby="form-modal-title"
                    aria-describedby="form-modal-description">
                    <Box sx={style}>
                        <Typography align="left" id="form-modal-title" variant="h6" component="h2">
                            More information required
                        </Typography>
                        {props.rccOption ? rccOptionQuestions() : portOptionQuestions()}
                        <div style={{float: 'left'}}>
                            <Button variant="contained"
                                    disabled={!enableSubmitRequest()}
                                    onClick={handleEvent}>Submit request</Button>
                        </div>
                        <Button style={{float: 'right'}} onClick={handleClose}>Cancel</Button>
                    </Box>
                </Modal>
            </div>
        </div>
    );
}
