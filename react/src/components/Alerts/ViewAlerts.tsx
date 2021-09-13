import React, {useEffect} from "react";
import moment from "moment-timezone";
import axios from "axios";
import AlertLike from "../../model/Alert";
import {Box, Button, Container, Typography} from "@material-ui/core";
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
    },
    button: {
      marginBottom: theme.spacing(1)
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
        {props.alert.title !== "" ? props.alert.title + " - " : ""}{props.alert.message}
        </span>
}

function ListAlerts() {

  const classes = useStyles();

  const [state, setState] = React.useState({
    portAlerts: {} as IPortAlerts
  });

  const refreshAlerts = () => axios
    .get("/api/alerts")
    .then(response => setState({
      portAlerts: response.data as IPortAlerts
    }));

  useEffect(() => {
    refreshAlerts();
  }, [])

  const clearAlertForPort = (portCode: String) =>
    () => axios.delete("/api/alerts/" + portCode).then(refreshAlerts)

  const portAlerts = []
  for (let portCode in state.portAlerts) {
    if (state.portAlerts.hasOwnProperty(portCode)) {
      portAlerts.push(
        <Box key={portAlerts.length}>
          <h1>{portCode}</h1>
          <Button className={classes.button} onClick={clearAlertForPort(portCode)} color={"secondary"}
                  variant={"contained"}>Clear alerts for {portCode}</Button>
          {state.portAlerts[portCode].map((alert: AlertLike) =>
            (
              <div>
                <Alert alert={alert}/>
                <Typography component={"span"} align={"right"}>
                  Expires: {moment(alert.expires).format("DD-MM-YYYY HH:mm")}
                </Typography>
              </div>
            )
          )}
        </Box>
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
