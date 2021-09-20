import React from "react";
import {CircularProgress, Grid} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles({
  root: {
    background: "#fff",
    minWidth: "100%",
    minHeight: "100vh",
    display: "flex",
    flexDirection: "column",
    justifyContent: "center"
  }
})

export default () => {
  const classes = useStyles()

  return <Grid
    container={true}
    className={classes.root}
    spacing={0}
    alignItems="center"
    justify="center"
  >
    <CircularProgress/>
  </Grid>
}


