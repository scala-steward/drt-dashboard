import {Box, Button, Menu, MenuItem} from "@mui/material";
import {styled} from '@mui/material/styles';
import React from "react";
import {Link} from "react-router-dom";
import {UserProfile} from "../model/User";

const TriggerButton = styled(Button)(({theme}) => ({
    marginTop: theme.spacing(-1),
    padding: theme.spacing(0),
}));

interface IProps {
    logoutLink: string,
    user: UserProfile
}

export default function Navigation(props: IProps) {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    return (
        <Box>
            <TriggerButton
                aria-controls="navigation"
                aria-haspopup="true"
                variant={"contained"}
                onClick={handleClick}
                sx={{marginTop: '0px'}}
            >
                Menu
            </TriggerButton>
            <Menu
                id="navigation"
                anchorEl={anchorEl}
                keepMounted
                open={!!anchorEl}
                onClose={handleClose}
            >
                <MenuItem onClick={handleClose}><Link to="/">Home</Link></MenuItem>
                {props.user.roles.includes("manage-users") ?
                    <MenuItem onClick={handleClose}><Link to="/userManagement">Access Requests</Link></MenuItem> : ""}
                {props.user.roles.includes("create-alerts") ?
                    <MenuItem onClick={handleClose}><Link to="/alerts">Alerts</Link></MenuItem> : ""}
                {props.user.roles.includes("manage-users") ?
                    <MenuItem onClick={handleClose}><Link to="/feature-guide-upload">Feature
                        Guides</Link></MenuItem> : ""}
                {props.user.roles.includes("manage-users") ?
                    <MenuItem onClick={handleClose}><Link to="/drop-ins/list">Drop-ins</Link></MenuItem> : ""}
                {props.user.roles.includes("manage-users") ?
                    <MenuItem onClick={handleClose}><Link to="/userTracking">Users</Link></MenuItem> : ""}
                <MenuItem onClick={handleClose}><a href={props.logoutLink} id="proposition-name">Log out</a></MenuItem>
            </Menu>
        </Box>
    );
}
