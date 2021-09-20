import React from "react";
import {Container, Tab, Tabs} from "@material-ui/core";

import {createStyles, makeStyles} from '@material-ui/core/styles';
import moment from "moment-timezone";
import {a11yProps, TabPanel} from "../TabPanel";
import SaveAlert from "./SaveAlert";
import {ListAlerts} from "./ViewAlerts";
import {UserProfile} from "../../model/User";

moment.locale("en-gb");

interface IProps {
  user: UserProfile;
}

const useStyles = makeStyles(() =>
  createStyles({
    container: {
      textAlign: "left"
    }
  }),
);

export default function Alerts(props: IProps) {

  const classes = useStyles();

  const [selectedTab, setSelectedTab] = React.useState(0);

  const changeTabs = (event: React.ChangeEvent<{}>, newValue: number) => setSelectedTab(newValue);

  return <div>
    <Tabs value={selectedTab} onChange={changeTabs} aria-label="simple tabs example">
      <Tab label="Add Alert" {...a11yProps(0)} />
      <Tab label="View Alerts" {...a11yProps(1)} />
    </Tabs>
    <TabPanel value={selectedTab} index={0}>
      <SaveAlert user={props.user} callback={() => setSelectedTab(1)}/>
    </TabPanel>
    <TabPanel index={1} value={selectedTab}>
      <ListAlerts/>
    </TabPanel>
  </div>;
}
