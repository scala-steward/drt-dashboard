import * as React from 'react';
import { Theme, useTheme } from '@mui/material/styles';
import {Box, Grid, FormControl, List, ListItem, Select, Chip, MenuItem, OutlinedInput, InputLabel, Accordion, AccordionSummary, AccordionDetails,FormControlLabel, Checkbox, SelectChangeEvent} from "@mui/material";
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { Cancel } from '@mui/icons-material';

interface Region {
  name: string;
  ports: string[];
}

interface DownloadPortsProps {
  error: boolean,
  handlePortChange: (event: SelectChangeEvent<string[]>) => void,
  handlePortCheckboxChange: (event: React.ChangeEvent<HTMLInputElement>) => void,
  handlePortCheckboxGroupChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  handleRemovePort: (port:string) => void;
  portsByRegion: Region[],
  selectedPorts: string[],
}

const ITEM_HEIGHT = 48;
const ITEM_PADDING_TOP = 8;
const MenuProps = {
  PaperProps: {
    style: {
      maxHeight: ITEM_HEIGHT * 4.5 + ITEM_PADDING_TOP,
      width: 250,
    },
  },
};

function getStyles(port: string, selectedPorts: readonly string[], theme: Theme) {
  return {
    fontWeight:
    selectedPorts.indexOf(port) === -1
        ? theme.typography.fontWeightRegular
        : theme.typography.fontWeightMedium,
  };
}

export default function DownloadPorts({error, handlePortChange, handleRemovePort, handlePortCheckboxChange, handlePortCheckboxGroupChange, portsByRegion, selectedPorts  }: DownloadPortsProps) {
  const theme = useTheme();
  const [expandedTab, setExpandedTab] = React.useState<string>('');

  const allUserPorts :string[] = portsByRegion.map((region) => [...region.ports]).flat();
  portsByRegion.forEach((region) => region.ports.sort());

  const renderPortColumn = (airport: string) => {
    return (
      <ListItem key={airport}><FormControlLabel key={airport} control={<Checkbox name={airport} checked={selectedPorts.includes(airport)} onChange={handlePortCheckboxChange} inputProps={{ 'aria-label': 'controlled' }} />} label={airport} /></ListItem>
    )
  }

  const accordionChange = (event: React.SyntheticEvent, isClosed: boolean, regionName: string, regionPorts: string[]) => {
    let target = (event.target as HTMLElement).tagName;

    switch (target) {
      case 'INPUT':
        //clicked checkbox
        if (selectedPorts.some(r=> regionPorts.includes(r))) {
          //user is unchecking region
          !isClosed && setExpandedTab('');
        } else {
          //user is checking region
          isClosed && setExpandedTab(regionName);
        }
        break;
      case 'DIV':
        //click accordion
        expandedTab === regionName ? setExpandedTab('') : setExpandedTab(regionName);
        break;
      default:
        break;
    }
  }

  return (
    <Box>
        <h3>Ports / Regions</h3>
        <FormControl sx={{width: '100%', backgroundColor: '#fff', marginBottom: '1em' }}>
          <InputLabel id="elected-ports-label">Ports</InputLabel>
          <Select
            error={error}
            labelId="selected-ports-label"
            id="selected-ports"
            multiple
            value={selectedPorts}
            onChange={handlePortChange}
            sx={{fontSize: '22px'}}
            input={<OutlinedInput id="select-multiple-chip" label="Chip" />}
            renderValue={(selected) => (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                {selected.map((value) => (
                  <Chip 
                    clickable
                    deleteIcon={
                      <Cancel
                        onMouseDown={(event) => event.stopPropagation()}
                      />
                    }
                    key={value} 
                    label={value} 
                    onDelete={() => handleRemovePort(value)}
                     />
                ))}
              </Box>
            )}
            MenuProps={MenuProps}
          >
            { allUserPorts.map((port) => 
              <MenuItem
                key={port}
                value={port}
                style={getStyles(port, selectedPorts, theme)}
              >
                {port}
              </MenuItem> )}
          </Select>
        </FormControl>
        <div>
        {
          portsByRegion.map((region: Region) => {
            return (
            <Accordion 
              key={region.name} 
              expanded={expandedTab === region.name} 
              onChange={(event, isClosed) => accordionChange(event, isClosed, region.name, region.ports)}
              >
              <AccordionSummary
                expandIcon={<ExpandMoreIcon />}
                aria-controls="panel1a-content"
                id="panel1a-header"
                sx={{margin: '12px 0 !important'}}
              >
                  <FormControlLabel control={<Checkbox name={region.name} checked={selectedPorts.some(r=> region.ports.includes(r))} onChange={handlePortCheckboxGroupChange} inputProps={{ 'aria-label': 'controlled' }} />} label={region.name} />
                  <Chip sx={{marginLeft: '1em'}} label={`${region.ports.filter(port => selectedPorts.includes(port)).length} selected`} />

              </AccordionSummary>
              <AccordionDetails>
                <Grid container spacing={4}>
                  <Grid item>
                    <List sx={{paddingRight: 1, borderRight: region.ports.length > 4 ? '1px solid #eee': 'none'}}>
                      { region.ports.slice(0,4).map((airport: string) => renderPortColumn(airport))}
                    </List>
                  </Grid>
                  <Grid item>
                    { region.ports.length > 4 && <List sx={{paddingRight: 1, borderRight: region.ports.length > 8 ? '1px solid #eee' : ''}}>
                      { region.ports.slice(4,8).map((airport: string) => renderPortColumn(airport))}
                    </List>}
                  </Grid>
                  <Grid item>
                    { region.ports.length > 8 && <List sx={{paddingRight: 1, borderRight: '1px solid #eee'}}>
                      { region.ports.slice(8,12).map((airport: string) => renderPortColumn(airport))}
                    </List> }
                  </Grid>
                  <Grid item>
                    { region.ports.length > 12 && <List>
                      { region.ports.slice(12,16).map((airport: string) => renderPortColumn(airport))}
                    </List> }
                  </Grid>
                </Grid>
                
              </AccordionDetails>
            </Accordion>)
          })
        }
        </div>
    </Box>
  )
}
