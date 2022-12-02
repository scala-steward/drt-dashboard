import * as React from 'react';
import Radio from '@mui/material/Radio';
import RadioGroup from '@mui/material/RadioGroup';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormControl from '@mui/material/FormControl';
import FormLabel from '@mui/material/FormLabel';
import {Box} from "@mui/material";

interface IProps {
  handleRccOptionCallback: (isRccUser: boolean) => void;
}

export default function InitialRequestForm(props: IProps) {
  return (
    <Box sx={{width: '100%'}}>
      <FormControl>
        <FormLabel id='initial-radio-buttons-group-label'><b>I require access to</b></FormLabel>
        <RadioGroup
          aria-labelledby='initial-radio-buttons-group-label'
          name='radio-buttons-group'
          defaultValue='port'
          onChange={event => {
            const updatedValue = event.target.value === 'rccu';
            // setIsRccUser(updatedValue);
            props.handleRccOptionCallback(updatedValue)
          }}
        >
          <FormControlLabel value='port' control={<Radio/>} label='Port level data'/>
          <FormControlLabel value='rccu' control={<Radio/>} label='RCC level data'/>
        </RadioGroup>
      </FormControl>
    </Box>
  );
}
