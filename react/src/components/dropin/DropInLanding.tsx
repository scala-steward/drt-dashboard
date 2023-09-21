import {CreateDropIn} from "./CreateDropIn";
import React from "react";
import {ListDropIns} from "./ListDropIns";
import {EditDropIn} from "./EditDropIn";
import {
    BrowserRouter as Router,
    Route,
    Switch,
    Link, useLocation,
} from "react-router-dom";
import {RegisteredUsers} from "./RegisteredUsers";
import {Breadcrumbs} from "@mui/material";
import Typography from "@mui/material/Typography";

export function DropInLanding() {
    const breadcrumbNameMap: { [key: string]: string } = {
        '/drop-ins': 'Drop-ins',
        '/drop-ins/new': 'Create',
        '/drop-ins/edit/:dropInId': 'Edit',
        '/drop-ins/list': 'List',
        '/drop-ins/list/save': 'List',
        '/drop-ins/list/registeredUsers/:dropInId': 'registrations',
    };

    const SeminarsBreadcrumbs: React.FC = () => {
        const location = useLocation();
        const pathnames = location.pathname.split('/').filter((x) => x);

        return (
            <Breadcrumbs aria-label="breadcrumb">
                {pathnames.map((value, index) => {
                    const to = `/${pathnames.slice(0, index + 1).join('/')}`;
                    let name = breadcrumbNameMap[to];

                    if (!name) {
                        for (const key in breadcrumbNameMap) {
                            const regexPath = '^' + key.split('/').map(segment => {
                                if (segment.startsWith(':')) {
                                    return '[^/]+';
                                } else if (segment === '*') {
                                    return '.*';
                                }
                                return segment;
                            }).join('/') + '$';

                            if (new RegExp(regexPath).test(to)) {
                                name = breadcrumbNameMap[key];
                                break;
                            }
                        }
                    }

                    if (!name) return null;

                    const last = index === pathnames.length - 1;

                    return last ? (
                        <Typography color="textPrimary" key={to}>
                            {name}
                        </Typography>
                    ) : (
                        <Link color="inherit" to={to} key={to}>
                            {name}
                        </Link>
                    );
                })}
            </Breadcrumbs>
        );
    };

    return (
        <div>
            <Router>
                <SeminarsBreadcrumbs/>
                <Switch>
                    <Route exact path="/drop-ins" component={ListDropIns}/>
                    <Route exact path="/drop-ins/list/crud/:operations" component={ListDropIns}/>
                    <Route exact path="/drop-ins/list/:listAll?" component={ListDropIns}/>
                    <Route exact path="/drop-ins/new" component={CreateDropIn}/>
                    <Route exact path="/drop-ins/edit/:dropInId" component={EditDropIn}/>
                    <Route exact path="/drop-ins/list/registeredUsers/:dropInId" component={RegisteredUsers}/>
                </Switch>
            </Router>
        </div>
    )
}
