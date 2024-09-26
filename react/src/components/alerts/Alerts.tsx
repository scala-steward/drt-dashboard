import React from "react";
import {Breadcrumbs, Stack, Tab, Tabs} from "@mui/material";
import moment from "moment-timezone";
import {a11yProps, TabPanel} from "../TabPanel";
import AlertForm from "./AlertForm";
import {ListAlerts} from "./ViewAlerts";
import {UserProfile} from "../../model/User";
import {PortRegion} from "../../model/Config";
import {Link} from "react-router-dom";
import Typography from "@mui/material/Typography";
import {Helmet} from "react-helmet";
import {adminPageTitleSuffix} from "../../utils/common";

moment.locale("en-gb");

interface IProps {
  regions: PortRegion[]
  user: UserProfile;
}

export default function Alerts(props: IProps) {

  const [selectedTab, setSelectedTab] = React.useState(0);

  const changeTabs = (event: React.ChangeEvent<any>, newValue: number) => setSelectedTab(newValue);

  return <>
    <Helmet>
      <title>Alerts {adminPageTitleSuffix}</title>
    </Helmet>
    <Stack gap={4} alignItems={'stretch'} sx={{mt: 2}}>
      <Breadcrumbs>
        <Link to={"/"}>
          Home
        </Link>
        <Typography color="text.primary">Alerts</Typography>
      </Breadcrumbs>
      <Tabs value={selectedTab} onChange={changeTabs} aria-label="simple tabs example">
        <Tab label="Add Alert" {...a11yProps(0)} />
        <Tab label="View Alerts" {...a11yProps(1)} />
      </Tabs>
      <TabPanel value={selectedTab} index={0}>
        <AlertForm regions={props.regions} user={props.user} callback={() => setSelectedTab(1)}/>
      </TabPanel>
      <TabPanel index={1} value={selectedTab}>
        <ListAlerts/>
      </TabPanel>
    </Stack>
  </>
}
