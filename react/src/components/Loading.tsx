import React from "react";
import { styled } from '@mui/material/styles';
import {CircularProgress, Grid} from "@mui/material";

const PREFIX = 'Loading';

const classes = {
  root: `${PREFIX}-root`
};

const StyledGrid = styled(Grid)({
  [`&.${classes.root}`]: {
    background: "#fff",
    minWidth: "100%",
    minHeight: "100vh",
    display: "flex",
    flexDirection: "column",
    justifyContent: "center"
  }
});

export default () => {
  return (
    <StyledGrid
      container={true}
      className={classes.root}
      spacing={0}
      alignItems="center"
      justifyContent="center"
    >
      <CircularProgress/>
    </StyledGrid>
  );
}


