import React from 'react';
import axios from 'axios';
import Button from '@material-ui/core/Button';

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
  domain: string;
}

interface IProps {
}

interface IState {
  config?: Config;
  user?: User;
  portsRequested: string[];
  requestFinished: boolean;
}

export default class Home extends React.Component<IProps, IState> {
  constructor(props: IProps) {
    super(props);

    this.state = {
      portsRequested: [],
      requestFinished: false
    }

    this.handlePortSelectionChange = this.handlePortSelectionChange.bind(this)
    this.handleAccessRequest = this.handleAccessRequest.bind(this)
  }

  componentDidMount() {
    axios
      .get("/api/user")
      .then(res => {
        let user = res.data as UserLike;
        this.setState({...this.state, user: user})
      })
      .catch(t => console.log('caught: ' + t))

    axios
      .get("/api/config")
      .then(res => {
        let config = res.data as Config;
        console.log("config ports: " + config.ports)
        this.setState({...this.state, config: config})
      })
      .catch(t => console.log('caught: ' + t))
  }

  handlePortSelectionChange(event: React.ChangeEvent<HTMLInputElement>) {
    const target = event.target;
    const checked = target.checked;
    const port = target.name;

    let newPortsRequested: string[] = this.state.portsRequested

    if (checked) newPortsRequested.push(port)
    else newPortsRequested = this.state.portsRequested.filter(p => p !== port)

    this.setState({...this.state, portsRequested: newPortsRequested});
  }

  handleAccessRequest(_: React.MouseEvent<HTMLButtonElement>) {
    axios
      .post("/api/request-access", {ports: this.state.portsRequested})
      .then(_ => {
        this.setState({...this.state, requestFinished: true})
      })
      .catch(t => console.log('caught: ' + t))
  }

  render() {
    let stuff;

    if (this.state.user === undefined)
      stuff = <p>Loading..</p>
    else if (this.state.requestFinished)
      stuff = <div>Thanks for your request. We'll get back to you shortly</div>
    else if (this.state.user.ports.length === 0)
      stuff = <div>
        <p>Please select the ports you require access to</p>
        <ul>
          {this.state.config?.ports.map((portCode) => {
            return <li key={portCode}>
              <input
                id={portCode}
                name={portCode}
                type="checkbox"
                checked={this.state.portsRequested.includes(portCode)}
                onChange={this.handlePortSelectionChange}>
              </input><label htmlFor={portCode}>{portCode.toUpperCase()}</label></li>
          })}
        </ul>

        <Button disabled={this.state.portsRequested.length === 0}
                onClick={this.handleAccessRequest}
                variant="contained"
                color="primary">
          Request access
        </Button>
      </div>
    else
      stuff = <div><p>Select your destination</p>
        <ul>
          {this.state.user.ports.map((portCode) => {
            const domain = this.state.config?.domain;
            const url = 'https://' + portCode + '.' + domain;
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
