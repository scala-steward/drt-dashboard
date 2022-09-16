import {Box} from "@mui/material";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Checkbox from "@mui/material/Checkbox";
import ListItemText from "@mui/material/ListItemText";
import React from "react";
import {PortRegion} from "../model/Config";

interface IProps {
    portDisabled: boolean
    regions: PortRegion[]
    selectedPorts: string[]
    setPorts: ((value: (((prevState: string[]) => string[]) | string[])) => void)
    selectedRegions: string[]
    setSelectedRegions: ((value: (((prevState: string[]) => string[]) | string[])) => void)
}

export const PortsByRegionCheckboxes = (props: IProps) => {
    const updatePortSelection = (port: string) => {
        const requested: string[] = props.selectedPorts.includes(port) ?
            props.selectedPorts.filter(value => value !== port) : [...props.selectedPorts, port]
        props.setPorts(requested);
    };

    const allPorts = props.regions.map(r => r.ports).reduce((a, b) => a.concat(b))
    const allPortsCount = props.regions.reduce((a, b) => a + b.ports.length, 0)
    const allPortsSelected = props.selectedPorts.length === allPortsCount
    const allRegions = props.regions.map(r => r.name)

    const updateRegionSelection = (region: string) => {
        const requested: string[] = props.selectedRegions.includes(region) ?
            props.selectedRegions.filter(value => value !== region) : [...props.selectedRegions, region]
        props.setSelectedRegions(requested);
    };

    return <Box sx={{
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-start',
        alignContent: 'top'
    }}>
        <List>
            <ListItem
                onClick={() => {
                    if (!allPortsSelected) {
                        props.setPorts(allPorts)
                        props.setSelectedRegions(allRegions)
                    } else {
                        props.setSelectedRegions([])
                        props.setPorts([])
                    }
                }}>
                <ListItemIcon>
                    <Checkbox
                        inputProps={{'aria-labelledby': "allPorts"}}
                        name="allPorts"
                        checked={props.selectedPorts.length === allPortsCount}
                        indeterminate={props.selectedPorts.length > 0 && props.selectedPorts.length < allPortsCount}
                    />
                </ListItemIcon>
                <ListItemText id="allPorts">
                    <Box sx={{fontWeight: 'bold'}}>All regions</Box>
                </ListItemText>
            </ListItem>
            <ListItem alignItems='flex-start'>
                {props.regions.map((region) => {
                    const sortedPorts = [...region.ports].sort()
                    const regionSelected = sortedPorts.every(p => props.selectedPorts.includes(p))
                    const regionPartiallySelected = sortedPorts.some(p => props.selectedPorts.includes(p)) && !regionSelected

                    function toggleRegionPorts(regionName: string) {
                        updateRegionSelection(regionName)
                        if (!regionSelected) props.setPorts(props.selectedPorts.concat(sortedPorts))
                        else props.setPorts(props.selectedPorts.filter(p => !sortedPorts.includes(p)))
                    }

                    return <Box sx={{
                        minWidth: '200px',
                        marginRight: '50px'
                    }}>
                        <List>
                            <ListItem
                                button
                                onClick={() => toggleRegionPorts(region.name)}
                                key={region.name}
                                disablePadding={true}
                            >
                                <ListItemIcon>
                                    <Checkbox
                                        inputProps={{'aria-labelledby': region.name}}
                                        name={region.name}
                                        checked={props.selectedRegions && props.selectedRegions.includes(region.name)}
                                        indeterminate={regionPartiallySelected}
                                    />
                                </ListItemIcon>
                                <ListItemText>
                                    <Box sx={{fontWeight: 'bold'}}>{region.name}</Box>
                                </ListItemText>
                            </ListItem>
                            {sortedPorts.map((portCode) => {
                                return <ListItem
                                    button
                                    key={portCode}
                                    disabled={props.portDisabled}
                                    onClick={() => updatePortSelection(portCode)}
                                    disablePadding={true}
                                >
                                    <ListItemIcon>
                                        <Checkbox
                                            inputProps={{'aria-labelledby': portCode}}
                                            name={portCode}
                                            checked={props.selectedPorts.includes(portCode)}
                                        />
                                    </ListItemIcon>
                                    <ListItemText id={portCode} primary={portCode.toUpperCase()}/>
                                </ListItem>
                            })}
                        </List>
                    </Box>
                })}
            </ListItem>
        </List>
    </Box>
}
