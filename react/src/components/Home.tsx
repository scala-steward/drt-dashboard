import React from 'react';

import axios from 'axios';

interface UserLike {
  email: string;
  ports: string[];
}

class User implements UserLike {
  email: string;
  ports: string[];

  constructor(email: string, ports: string[]) {
    this.email = email;
    this.ports = ports;
  }
}

interface Config {
  ports: string[];
  drtDomain: string;
}

interface IProps {
}

interface IState {
  config?: Config;
  user?: User;
}

export default class Home extends React.Component<IProps, IState> {
  constructor(props: IProps) {
    super(props);

    this.state = {
      user: undefined
    };
  }

  componentDidMount() {
    axios
      .get("/api/user")
      .then(res => this.setState({user: res.data as UserLike}))
      .catch(t => console.log('caught: ' + t))
    axios
      .get("/api/config")
      .then(res => this.setState({config: res.data as Config}))
      .catch(t => console.log('caught: ' + t))
  }

  render() {
    let stuff;
    const domain = this.state.config?.drtDomain;

    if (this.state.user === undefined)
      stuff = <p>Loading..</p>
    else if (this.state.user.ports.length === 0)
      stuff = <p>
        Please select the ports you require access to

      </p>
    else
      stuff = <div><p>Select your destination</p>
        <ul>
          {this.state.user.ports.map((portCode) => {
            const url = 'https://' + {portCode} + '.' + {domain};
            return <li key={portCode}><a href={url}>{portCode}</a></li>
          })}
        </ul>
      </div>

    return (
      <header className="App-header">
        <h1>Welcome to DRT</h1>
        {stuff}
      </header>
    )
  }
}
