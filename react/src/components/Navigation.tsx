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
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem onClick={handleClose}><Link to="/">Home</Link></MenuItem>
        {props.user.roles.includes("create-alerts") ?
          <MenuItem onClick={handleClose}><Link to="/alerts">Alerts</Link></MenuItem> : ""}
        {props.user.roles.includes("nebo:upload") ?
          <MenuItem onClick={handleClose}><Link to="/upload">Nebo Upload</Link></MenuItem> : ""}
        {props.user.roles.includes("red-lists:edit") ?
          <MenuItem onClick={handleClose}><Link to="/red-list-editor">Edit red list</Link></MenuItem> : ""}
        <MenuItem onClick={handleClose}><a href={props.logoutLink} id="proposition-name">Log out</a></MenuItem>
      </Menu>
    </Box>
  );
}

