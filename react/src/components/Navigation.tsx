import {Button, Menu, MenuItem} from "@material-ui/core";
import React from "react";
import {Link} from "react-router-dom";
import {createStyles, makeStyles, Theme} from "@material-ui/core/styles";
import UserLike from "../model/User";

interface IProps {
    logoutLink: string,
    user: UserLike
}

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        trigger: {
            marginTop: theme.spacing(-1),
            padding: theme.spacing(0),
        }
    }),
);

export default function Navigation(props: IProps) {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

    const classes = useStyles();
    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
        setAnchorEl(null);
    };

    return (
        <div>
            <Button
                className={classes.trigger}
                aria-controls="navigation"
                aria-haspopup="true"
                variant={"contained"}
                onClick={handleClick}
            >Menu</Button>
            <Menu
                id="navigation"
                anchorEl={anchorEl}
                keepMounted
                open={Boolean(anchorEl)}
                onClose={handleClose}
            >

                <MenuItem onClick={handleClose}><Link to="/">Home</Link></MenuItem>
                {

                    props.user.roles.includes("create-alerts") ?
                        <MenuItem onClick={handleClose}><Link to="/alerts">Alerts</Link></MenuItem> : ""
                }
                <MenuItem onClick={handleClose}><Link to="/upload">Upload</Link></MenuItem>
                <MenuItem onClick={handleClose}><a href={props.logoutLink} id="proposition-name">Log out</a></MenuItem>
            </Menu>
        </div>
    );
}

