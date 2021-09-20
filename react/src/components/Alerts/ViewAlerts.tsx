import React, {useEffect} from "react";
import moment from "moment-timezone";
import AlertLike from "../../model/Alert";
import {Box, Button, Container, Typography} from "@material-ui/core";
import {createStyles, makeStyles, Theme} from "@material-ui/core/styles";
import {rootStore} from "../../store/rootReducer";
import {deleteAlertsForPort, fetchAlerts} from "../../store/alertsSlice";

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

export interface PortAlerts {
  portCode: string
  alerts: AlertLike[]
}

interface PortAlertsLoaded {
  kind: "PortAlertsLoaded"
  portAlerts: PortAlerts[]
}

interface PortAlertsPending {
  kind: "PortAlertsPending"
}

interface PortAlertsNotLoaded {
  kind: "PortAlertsNotLoaded"
}

type AllPortAlerts = PortAlertsNotLoaded | PortAlertsLoaded | PortAlertsPending

const Alert = (props: iAlertProps) => {

  const classes = useStyles();

  const alertClass = props.alert.alertClass === "notice" ? classes.notice : classes.warning

  return props.alert.title + props.alert.message === "" ? null :
    <span role="alert" className={classes.alert + " " + alertClass}>
        {props.alert.title !== "" ? props.alert.title + " - " : ""}{props.alert.message}
        </span>
}

function ListAlerts() {

  const classes = useStyles();

  const [allPortAlerts, setAllPortAlerts] = React.useState<AllPortAlerts>({kind: "PortAlertsNotLoaded"});

  useEffect(() => {
    if (allPortAlerts.kind === 'PortAlertsNotLoaded') {
      const d = rootStore.dispatch(fetchAlerts((a: PortAlerts[]) => setAllPortAlerts({
        kind: "PortAlertsLoaded",
        portAlerts: a
      })))
      return () => d.abort()
    }
  }, [allPortAlerts, setAllPortAlerts])

  const clearAlertForPort = (portCode: string) => () =>
    rootStore.dispatch(deleteAlertsForPort({
      portCode: portCode, onSuccess: () => {
        allPortAlerts.kind === 'PortAlertsLoaded' && setAllPortAlerts({
          ...allPortAlerts,
          portAlerts: allPortAlerts.portAlerts.filter(pa => pa.portCode !== portCode)
        })
      }
    }))

  return <Container>{allPortAlerts.kind === 'PortAlertsLoaded' && allPortAlerts.portAlerts.map(portAlerts => {
    return <Box key={portAlerts.portCode}>
      <h1>{portAlerts.portCode}</h1>
      <Button className={classes.button} onClick={clearAlertForPort(portAlerts.portCode)} color={"secondary"}
              variant={"contained"}>Clear alerts for {portAlerts.portCode}</Button>
      {portAlerts.alerts.map((alert: AlertLike) =>
        <div>
          <Alert alert={alert}/>
          <Typography component={"span"} align={"right"}>
            Expires: {moment(alert.expires).format("DD-MM-YYYY HH:mm")}
          </Typography>
        </div>
      )}
    </Box>
  })
  }</Container>
}

export {ListAlerts, Alert};
