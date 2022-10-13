import React from 'react';
import {rest} from 'msw'
import {setupServer} from 'msw/node'
import {render, screen, waitFor} from '@testing-library/react';
import ApiClient from "../../services/ApiClient";
import App from "../../App";


function newServer(userPorts: string[], allPorts: string[]) {
  return setupServer(
    rest.get(ApiClient.userEndPoint, (req, res, ctx) => {
      return res(ctx.json({ports: userPorts, roles: userPorts, email: 'someone@drt'}))
    }),
    rest.get(ApiClient.configEndPoint, (req, res, ctx) => {
      return res(ctx.json({ports: allPorts, domain: 'drt.localhost'}))
    })
  );
}

describe('<AccessRequestForm />', () => {
  it('Lists all ports for access request when user has no port access', async () => {
    const server = newServer([], ['lhr', 'bhx'])
    server.listen();

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Please select the ports you require access to'));
      expect(screen.getByText('LHR'));
      expect(screen.getByText('BHX'));
    });

    server.close();
  });

  it('Lists user\'s ports to choose user has access to some ports', async () => {
    const server = newServer(['lhr', 'bhx'], ['lhr', 'bhx'])
    server.listen();

    render(<App />);

    await waitFor(() => {
      expect(screen.getByText('Select your destination'));
      expect(screen.getByText('LHR'));
      expect(screen.getByText('BHX'));
    });

    server.close();
  });
});
