import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import {BrowserRouter} from "react-router-dom";
import Alerts from "../../components/alerts/Alerts";
import {setupServer} from "msw/node";
import {rest} from "msw";
import {UserProfile} from "../../model/User";


describe.skip('<Alerts />', () => {
    const user: UserProfile = {
        ports: ["LHR", "BHX"],
        roles: ["LHR", "BHX", "create-alerts"],
        email: 'someone@drt'
    }

    describe("Alert Tabs", () => {

        it("contains tabs for add and view alerts", () => {
            render(<BrowserRouter><Alerts user={user} regions={[]}/></BrowserRouter>)
            expect(screen.getByText("Add Alert"))
            expect(screen.getByText("View Alerts"))
        });

        it("defaults to the add alert view", () => {

            render(<BrowserRouter><Alerts user={user} regions={[]}/></BrowserRouter>)
            expect(screen.getByText("Save"))
        })

        it("should switch to the alert list when you tap the View Alerts link", async () => {

            const server = setupServer(
                rest.get('/api/alerts', (req, res, ctx) => {
                    return res(ctx.json({
                        "LHR": [{
                            alertClass: "notice",
                            title: "title",
                            message: "message",
                            expires: 0
                        }]
                    }))
                })
            )
            server.listen()

            render(<BrowserRouter><Alerts user={user} regions={[]}/></BrowserRouter>)

            fireEvent.click(screen.getByText('View Alerts'))

            await waitFor(() => {
                expect(screen.getByText("LHR"))
            })

            server.close();
        })

    });

});
