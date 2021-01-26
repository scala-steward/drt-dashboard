import React, {useEffect} from "react";
import moment from "moment-timezone";
import axios from "axios";
import AlertLike from "../../model/Alert";
import {Button, Card, Container, Typography} from "@material-ui/core";
import {createStyles, makeStyles, Theme} from "@material-ui/core/styles";

interface IPortAlerts {
    [key: string]: AlertLike[]
}

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        alert: {
            display: "block",
            padding: "3px 10px",
            marginBottom: "3px",
            border: "1px solid transparent",
            borderRadius: "4px"
        },
        notice: {
            background: "#adf"
        },
        warning: {
            background: "#faa"
        }
    }),
)

interface iAlertProps {
    alert: AlertLike
}

function Alert(props: iAlertProps) {

    const classes = useStyles();

    const alertClass = props.alert.alertClass === "notice" ? classes.notice : classes.warning

    return props.alert.title + props.alert.message === "" ? null :
        <span role="alert" className={classes.alert + " " + alertClass}>
        <p>{props.alert.title !== "" ? props.alert.title + " - " : ""}{props.alert.message}</p>
        </span>
}

function ListAlerts() {


    const [state, setState] = React.useState({
        portAlerts: {} as IPortAlerts
    });

    function refreshAlerts() {
        axios.get("/api/alerts").then(response => {
            const portAlerts = response.data as IPortAlerts

            setState({
                portAlerts: portAlerts
            })

        })
    }

    useEffect(() => {
        refreshAlerts();
    }, [])

    const clearAlertForPort = (portCode: String) =>
        () => axios.delete("/api/alerts/" + portCode).then(refreshAlerts)


    const portAlerts = []
    for (let portCode in state.portAlerts) {
        if (state.portAlerts.hasOwnProperty(portCode)) {
            portAlerts.push(
                <div>
                    <h1>{portCode}</h1>
                    <Button onClick={clearAlertForPort(portCode)}>Clear alerts for {portCode}</Button>
                    {state.portAlerts[portCode].map((alert: AlertLike) =>
                        (
                            <div>
                                <Alert alert={alert}/>
                                <Typography
                                    align={"right"}>Expires: {moment(alert.expires).format("DD-MM-YYYY HH:mm")}</Typography>
                            </div>
                        )
                    )}
                </div>
            )
        }
    }


    return (
        <Container>

            {portAlerts}
        </Container>
    )
}

export {ListAlerts, Alert};