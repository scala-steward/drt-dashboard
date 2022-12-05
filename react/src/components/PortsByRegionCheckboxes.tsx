import {Box, FormControlLabel} from "@mui/material";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import Checkbox from "@mui/material/Checkbox";
import React from "react";
import {PortRegion} from "../model/Config";

interface IProps {
  portDisabled: boolean
  regions: PortRegion[]
  onSelectedPortsChange: (ports: string[]) => void
  selectedPorts: string[]
}

export const PortsByRegionCheckboxes = (props: IProps) => {
  const allPorts = props.regions.map(r => r.ports).reduce((a, b) => a.concat(b))
  const allPortsCount = props.regions.reduce((a, b) => a + b.ports.length, 0)
  const allPortsSelected = props.selectedPorts.length === allPortsCount

  const regionIsSelected = (region: PortRegion, selectedPorts: string[]) => {
    return region.ports.every(p => selectedPorts.includes(p))
  }
  const regionIsPartiallySelected = (region: PortRegion, selectedPorts: string[]) => {
    const selectedPortsInRegion = region.ports.filter(p => selectedPorts.includes(p))
    return selectedPortsInRegion.length > 0 && selectedPortsInRegion.length < region.ports.length
  }
  const portsArePartiallySelected = () => {
    return props.selectedPorts.length > 0 && props.selectedPorts.length < allPortsCount
  }
  const removeRegionPorts = (region: PortRegion) => {
    return props.selectedPorts.filter(p => !region.ports.includes(p));
  }

  return <Box sx={{
    display: 'flex',
    flexWrap: 'wrap',
    justifyContent: 'space-start',
    alignContent: 'top'
  }}>
    <List>
      <ListItem>
        <FormControlLabel
          control={<Checkbox
            inputProps={{'aria-labelledby': "allPorts"}}
            name="allPorts"
            checked={props.selectedPorts.length === allPortsCount}
            indeterminate={portsArePartiallySelected()}
            onChange={() => {
              const updatedPorts = allPortsSelected ? [] : allPorts
              props.onSelectedPortsChange(updatedPorts)
            }}
          />}
          label={<Box sx={{fontWeight: 'bold'}}>All regions</Box>}
          sx={{fontWeight: 'bold'}}
        />
      </ListItem>
      <ListItem alignItems='flex-start'>
        {props.regions.map((region) => {
          const sortedPorts = [...region.ports].sort()

          return <Box
            key={region.name}
            sx={{
              minWidth: '200px',
              marginRight: '50px',
            }}
          >
            <List>
              <ListItem
                button
                key={region.name}
                disablePadding={true}
              >
                <FormControlLabel
                  control={<Checkbox
                    inputProps={{'aria-labelledby': region.name}}
                    name={region.name}
                    checked={regionIsSelected(region, props.selectedPorts)}
                    indeterminate={regionIsPartiallySelected(region, props.selectedPorts)}
                    onChange={(event) => {
                      const updatedPorts = event.target.checked ? removeRegionPorts(region).concat(region.ports) : removeRegionPorts(region)
                      props.onSelectedPortsChange(updatedPorts)
                    }}
                  />}
                  label={<Box sx={{fontWeight: 'bold'}}>{region.name}</Box>}
                />
              </ListItem>
              {sortedPorts.map((portCode) => {
                return <ListItem
                  button
                  key={portCode}
                  disabled={props.portDisabled}
                  disablePadding={true}
                >
                  <FormControlLabel
                    control={<Checkbox
                      inputProps={{'aria-labelledby': portCode}}
                      name={portCode}
                      checked={props.selectedPorts.includes(portCode)}
                      onChange={(event) => {
                        const updatedPorts = event.target.checked ? [...props.selectedPorts, portCode] : props.selectedPorts.filter(r => r != portCode)
                        props.onSelectedPortsChange(updatedPorts)
                      }}
                    />}
                    label={portCode.toUpperCase()}
                    sx={{fontWeight: 'bold'}}
                  />
                </ListItem>
              })}
            </List>
          </Box>
        })}
      </ListItem>
    </List>
  </Box>
}
