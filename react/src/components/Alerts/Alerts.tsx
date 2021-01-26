import React from "react";
import UserLike from "../../model/User";
import {Container, Tab, Tabs} from "@material-ui/core";

import {createStyles, makeStyles, Theme} from '@material-ui/core/styles';
import moment from "moment-timezone";
import {a11yProps, TabPanel} from "../TabPanel";
import SaveAlert from "./SaveAlert";
import {ListAlerts} from "./ViewAlerts";



moment.locale("en-gb");

interface IProps {
    user: UserLike;
}

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        container: {
            textAlign: "left"
        }
    }),
);

export default function Alerts(props: IProps) {

    const classes = useStyles();

    const [selectedTab, setSelectedTab] = React.useState(0);

    const changeTabs = (event: React.ChangeEvent<{}>, newValue: number) => {
        setSelectedTab(newValue);
    };


    return <Container className={classes.container}>
        <Tabs value={selectedTab} onChange={changeTabs} aria-label="simple tabs example">
            <Tab label="Add Alert" {...a11yProps(0)} />
            <Tab label="View Alerts" {...a11yProps(1)} />
        </Tabs>
        <TabPanel value={selectedTab} index={0}>
            <SaveAlert user={props.user} callback={() => setSelectedTab(1)} />
        </TabPanel>
        <TabPanel index={1} value={selectedTab}>
            <ListAlerts />
        </TabPanel>
    </Container>;
}
