import React from 'react';
import {fireEvent, render, screen, waitFor} from '@testing-library/react';
import Alerts from "../../components/Alerts/Alerts";
import UserLike from "../../model/User";
import {setupServer} from "msw/node";
import {rest} from "msw";


describe('<Alerts />', () => {
    const user: UserLike = {
        ports: ["LHR", "BHX"],
        roles: ["LHR", "BHX", "create-alerts"],
        email: 'someone@drt'
    }

    describe("Alert Tabs", () => {

        it("contains tabs for add and view alerts", () => {
            render(<Alerts user={user}/>)
            expect(screen.getByText("Add Alert"))
            expect(screen.getByText("View Alerts"))
        });

        it("defaults to the add alert view", () => {

            render(<Alerts user={user}/>)
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

            render(<Alerts user={user}/>)

            fireEvent.click(screen.getByText('View Alerts'))

            await waitFor(() => {
                expect(screen.getByText("LHR"))
            })

            server.close();
        })

    });

});
