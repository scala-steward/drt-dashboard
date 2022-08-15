import {Box} from "@mui/material";
import List from "@mui/material/List";
import ListItem from "@mui/material/ListItem";
import ListItemIcon from "@mui/material/ListItemIcon";
import Checkbox from "@mui/material/Checkbox";
import ListItemText from "@mui/material/ListItemText";
import React from "react";
import {PortRegion} from "../model/Config";


interface IProps {
    regions: PortRegion[]
    selectedRccuRegions: string[]
    setSelectedRccuRegions: ((value: (((prevState: string[]) => string[]) | string[])) => void)
}

export const RccuByRegionAccess = (props: IProps) => {

    const updateRccuByRegionSelection = (region: string) => {
        const requested: string[] = props.selectedRccuRegions.includes(region) ?
            props.selectedRccuRegions.filter(value => value !== region) : [...props.selectedRccuRegions, region]

        props.setSelectedRccuRegions(requested);
    };

    return <Box sx={{
        display: 'flex',
        flexWrap: 'wrap',
        justifyContent: 'space-start',
        alignContent: 'top'
    }}>
        <List>
            <ListItem key="rccu" alignItems='flex-start'>
                {props.regions.map((region) => {
                    return <Box sx={{
                        minWidth: '200px',
                        marginRight: '50px'
                    }}>
                        <List>
                            <ListItem
                                button
                                onClick={() => updateRccuByRegionSelection(region.name)}
                                key={region.name}
                                disablePadding={true}
                            >
                                <ListItemIcon>
                                    <Checkbox
                                        inputProps={{'aria-labelledby': region.name}}
                                        name={region.name}
                                        checked={props.selectedRccuRegions.includes(region.name)}
                                    />
                                </ListItemIcon>
                                <ListItemText>
                                    <Box>{region.name}</Box>
                                </ListItemText>
                            </ListItem>
                        </List>
                    </Box>
                })}
            </ListItem>
        </List>
    </Box>
}
