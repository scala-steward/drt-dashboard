import React from "react";
import {CircularProgress, Grid} from "@mui/material";
import {makeStyles} from "@mui/material/styles";

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


