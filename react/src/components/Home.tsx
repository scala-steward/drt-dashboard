import React from 'react';

import AccessRequestForm from "./AccessRequestForm";
import PortList from "./PortList";
import UserLike from "../model/User";
import ConfigLike from "../model/Config";
import {Box} from "@material-ui/core";


interface IProps {
    user: UserLike;
    config: ConfigLike;
}

export default function Home(props: IProps) {

    return <Box className="App-header">
        {props.user.ports.length === 0 ?
            <AccessRequestForm ports={props.config.ports} teamEmail={props.config.teamEmail}/> :
            <PortList ports={props.user.ports} drtDomain={props.config.domain}/>
        }
    </Box>
}

