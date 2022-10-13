import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import {KeyCloakUser} from './UserAccessCommon';

const style = {
    position: 'absolute' as 'absolute',
    top: '50%',
    left: '50%',
    transform: 'translate(-50%, -50%)',
    width: 600,
    bgcolor: 'background.paper',
    border: '2px solid #000',
    boxShadow: 24,
    p: 4,
};

interface IProps {
    userDetails: KeyCloakUser;
}

export default function UserAccessApproved(props: IProps) {

    const resetRequestPosted = () => {
        window.location.reload();
    }

    return (
        <div className="flex-container">
            <div>
                <Box sx={style}>
                    <Typography align="center" id="modal-modal-title" variant="h6" component="h2">
                        User Approved
                    </Typography>
                    <br/>
                    <div>Selected User with email {props.userDetails.email} is granted access
                        requested.
                    </div>
                    <Button style={{float: 'right'}} onClick={resetRequestPosted}>back</Button>
                </Box>
            </div>
        </div>
    );
}
