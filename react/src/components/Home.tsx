import React from 'react';

import axios from 'axios';

interface UserLike {
    ports: string[];
    isPoise: boolean;
}

class User implements UserLike {
    isPoise: boolean;
    ports: string[];

    constructor(isPoise: boolean, ports: string[]) {
        this.isPoise = isPoise;
        this.ports = ports;
    }
}

interface IProps {}

interface IState {
    isReady: boolean;
    user?: User;
}

export default class Home extends React.Component<IProps, IState> {
    constructor(props: IProps) {
        super(props);

        this.state = {
            isReady: false
        };
    }

    componentDidMount() {
        axios.get("/api/user")
            .then(res => {
                const user: UserLike = res.data as UserLike;
                console.log(user);
                this.setState({isReady: true, user: user});
            })
            .catch(t => {
                console.log('caught: ' + t)
            })
    }

    render() {
        let stuff;
        if (this.state.isReady)
            stuff = <p>Ready {this.state.user?.ports}</p>
        else
            stuff = <p>Not ready</p>

        return (
            <header className="App-header">
                <h1>Welcome to DRT</h1>
                {stuff}
            </header>
        )
    }
}
