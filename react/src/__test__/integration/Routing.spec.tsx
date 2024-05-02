import React, {ReactElement} from 'react';
import {render, screen, waitFor} from '@testing-library/react';
import {setupServer} from "msw/node";
import {rest} from "msw";
import ApiClient from "../../services/ApiClient";
import {App} from "../../App";
import {BrowserRouter} from "react-router-dom";


describe.skip('Dashboard routing', () => {
    function newServer(userPorts: string[], roles: string[], allPorts: string[]) {
        return setupServer(
            rest.get(ApiClient.userEndPoint, (req, res, ctx) => {
                return res(ctx.json({ports: userPorts, roles: roles.concat(userPorts), email: 'someone@drt'}))
            }),
            rest.get(ApiClient.configEndPoint, (req, res, ctx) => {
                return res(ctx.json({ports: allPorts, domain: 'drt.localhost'}))
            })
        );
    }

    const renderWithRouter = (ui: ReactElement, { route = '/' } = {}) => {
        window.history.pushState({}, 'Test page', route)

        return render(ui, { wrapper: BrowserRouter })
    }

    it('displays the home page for the / route', async () => {

        const server = newServer([],[], ['lhr', 'bhx'])
        server.listen();

        renderWithRouter(<App/>);

        await waitFor(() => {
            expect(screen.getByText("Welcome to DRT"));
        });

        server.close();
    });

    it('displays the alerts page for the /alerts route', async () => {

        const server = newServer([],[], ['lhr', 'bhx'])
        server.listen();

        renderWithRouter(<App/>, {"route" : "/alerts"});

        await waitFor(() => {
            expect(screen.getByText("Add Alert"));
            expect(screen.getByText("View Alerts"));
        });

        server.close();
    });
});
