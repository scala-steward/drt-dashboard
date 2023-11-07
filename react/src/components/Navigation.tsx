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

  const menuItems = [
    {label: 'Home', link: '/', roles: []},
    {label: 'Access requests', link: '/access-requests', roles: ['manage-users']},
    {label: 'Alert notices', link: '/alerts', roles: ['manage-users']},
    {label: 'Feature guides', link: '/feature-guide-upload', roles: ['manage-users']},
    {label: 'Health checks', link: '/health-checks', roles: ['manage-users']},
    {label: 'Drop-ins', link: '/drop-ins/list', roles: ['manage-users']},
    {label: 'Users', link: '/users', roles: ['manage-users']},
    {label: 'Log out', link: props.logoutLink, roles: []},
  ]

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
        {menuItems.map(item => {
          if (item.roles.length === 0 || item.roles.some(role => props.user.roles.includes(role))) {
            return <MenuItem onClick={handleClose} key={item.label}>
              <Link to={item.link}>{item.label}</Link>
            </MenuItem>
          }
        })}
      </Menu>
    </Box>
  );
}
