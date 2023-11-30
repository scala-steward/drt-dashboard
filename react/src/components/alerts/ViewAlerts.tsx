import React, {useEffect} from "react";
import moment from "moment-timezone";
import AlertLike from "../../model/Alert";
import {Box, Button, Container, Typography} from "@mui/material";
import {styled} from "@mui/material/styles";
import {deleteAlertsForPort, fetchAlerts} from "../../store/alerts";


const PREFIX = 'SaveAlert';

const classes = {
  button: `${PREFIX}-button`,
  alert: `${PREFIX}-alert`,
  notice: `${PREFIX}-notice`,
  warning: `${PREFIX}-warning`,
};

const StyledButton = styled(Button)(({theme}) => ({
  [`&.${classes.button}`]: {
    marginBottom: theme.spacing(1)
  },
}))

const StyledSpan = styled('span')(() => ({
  [`&.${classes.alert}`]: {
    display: "block",
    padding: "3px 10px",
    marginBottom: "3px",
    border: "1px solid transparent",
    borderRadius: "4px"
  },
  [`&.${classes.notice}`]: {
    background: "#adf"
  },
  [`&.${classes.warning}`]: {
    background: "#faa"
  },
}))

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

const Alert = (props: AlertLike) => {
  const alertClass = props.alertClass === "notice" ? classes.notice : classes.warning

  return props.title + props.message === "" ? null :
    <StyledSpan role="alert" className={classes.alert + " " + alertClass}>
      {props.title !== "" ? props.title + " - " : ""}{props.message}
    </StyledSpan>
}

function ListAlerts() {
  const [allPortAlerts, setAllPortAlerts] = React.useState<AllPortAlerts>({kind: "PortAlertsNotLoaded"});

  useEffect(() => {
    fetchAlerts((a: PortAlerts[]) => setAllPortAlerts({
      kind: "PortAlertsLoaded",
      portAlerts: a
    }))
  }, [])

  const clearAlertForPort = (portCode: string) => () =>
    deleteAlertsForPort({
      portCode: portCode, onSuccess: () => {
        allPortAlerts.kind === 'PortAlertsLoaded' && setAllPortAlerts({
          ...allPortAlerts,
          portAlerts: allPortAlerts.portAlerts.filter(pa => pa.portCode !== portCode)
        })
      }
    })

  return <Container>{allPortAlerts.kind === 'PortAlertsLoaded' && allPortAlerts.portAlerts.map(portAlerts => {
    return <Box key={portAlerts.portCode}>
      <h1>{portAlerts.portCode}</h1>
      <StyledButton className={classes.button} onClick={clearAlertForPort(portAlerts.portCode)} color={"secondary"}
                    variant={"contained"}>Clear alerts for {portAlerts.portCode}</StyledButton>
      {portAlerts.alerts.map((a: AlertLike) =>
        <div>
          <Alert alertClass={a.alertClass} expires={a.expires} message={a.message} title={a.title}/>
          <Typography component={"span"} align={"right"}>
            Expires: {moment(a.expires).format("DD-MM-YYYY HH:mm")}
          </Typography>
        </div>
      )}
    </Box>
  })
  }</Container>
}

export {ListAlerts, Alert};
