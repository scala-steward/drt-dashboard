import {Box, Button, Menu, MenuItem, Link as MuiLink} from "@mui/material";
import React from "react";
import {Link} from "react-router-dom";
import {UserProfile} from "../model/User";

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
    {label: 'Drop-in sessions', link: '/drop-in-sessions', roles: ['manage-users']},
    {label: 'Download Manager', link: '/download', roles: ['download-manager']},
    {label: 'National Dashboard', link: '/national-pressure', roles: ['forecast:view']},
    {label: 'Export Config', link: '/export-config', roles: ['manage-users']},
    {label: 'Feature guides', link: '/feature-guides', roles: ['manage-users']},
    {label: 'Health checks', link: '/health-checks', roles: ['health-checks:edit']},
    {label: 'Health check pauses', link: '/health-check-pauses', roles: ['health-checks:edit']},
    {label: 'Feedback', link: '/user-feedback', roles: ['manage-users']},
    {label: 'Users', link: '/users', roles: ['manage-users']},
  ]

  return (
    <Box>
      <Button sx={{"aria-controls": "navigation", marginTop: "-7px", "padding": "0px 15px" , fontWeight:"Bold"}} variant={"contained"}
              onClick={handleClick}> Menu </Button>
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
        <MenuItem onClick={handleClose} key={'logout'}>
          <MuiLink href={props.logoutLink}>Log out</MuiLink>
        </MenuItem>
      </Menu>
    </Box>
  );
}
