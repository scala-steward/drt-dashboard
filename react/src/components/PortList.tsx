import React from "react";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Icon from "@mui/icons-material/FlightLand";
import ListItemText from "@mui/material/ListItemText";
import Divider from "@mui/material/Divider";
import {Box, Grid} from "@mui/material";
import {styled} from "@mui/material/styles";


interface IProps {
  ports: string[];
  drtDomain: string;
}

const Region = styled('p')(() => ({
  fontSize: '21px',
  fontWeight: 'bold',
}));


export const PortList = (props: IProps) => {
  const regions =
    [
      {
        name: 'North',
        ports: ["BFS", "BHD", "DSA", "EDI", "GLA", "HUY", "LBA", "LPL", "MAN", "MME", "NCL", "PIK"]
      },
      {
        name: 'Central',
        ports: ["BHX", "EMA", "LCY", "LTN", "NYI", "STN"]
      },
      {
        name: 'South',
        ports: ["BOH", "BRS", "CWL", "EXT", "LGW", "NQY", "SOU"]
      },
      {
        name: 'Heathrow',
        ports: ["LHR"]
      }
    ]

  return <Box sx={{width: '100%'}}>
    <h1>Welcome to DRT</h1>
    <p>Select your destination</p>
    <Grid container spacing={2}>
      {regions.map((region) => {
        const sortedPorts = region.ports.sort()
        return <Grid item xs={3}>
          <Region>{region.name}</Region>
          <List>
            {sortedPorts.map((portCode) => {
              let portCodeLC = portCode.toLowerCase();
              const url = 'https://' + portCodeLC + '.' + props.drtDomain;
              return <div key={portCode}>
                <ListItem button component="a" href={url} id={"port-link-" + portCodeLC}>
                  <ListItemIcon><Icon/></ListItemIcon>
                  <ListItemText primary={portCode.toUpperCase()}/>
                </ListItem>
                <Divider variant="inset" component="li"/>
              </div>
            })}
          </List>
        </Grid>
      })}
    </Grid>

  </Box>

}
