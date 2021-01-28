import React from 'react';

import AccessRequestForm from "./AccessRequestForm";
import PortList from "./PortList";
import UserLike from "../model/User";
import ConfigLike from "../model/Config";


interface IProps {
    user: UserLike;
    config: ConfigLike;
}

export default function Home(props: IProps) {

    return <header className="App-header">
        <h1>Welcome to DRT</h1>
        {props.user.ports.length === 0 ?
            <AccessRequestForm ports={props.config.ports}/> :
            <PortList ports={props.user.ports} drtDomain={props.config.domain}/>
        }
    </header>
}

