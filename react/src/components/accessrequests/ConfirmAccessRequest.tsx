import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import ListItemText from "@mui/material/ListItemText";
import {ListItem} from "@mui/material";
import List from "@mui/material/List";

const style = {
  position: 'absolute' as 'absolute',
  top: '50%',
  left: '50%',
  transform: 'translate(-50%, -50%)',
  width: 600,
  bgcolor: 'background.paper',
  border: '2px solid #000',
  boxShadow: 24,
  p: 4,
};

interface IProps {
  emails: string[]
  message: string
  parentRequestPosted: boolean
  setParentRequestPosted: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  receivedUserDetails: boolean
  setReceivedUserDetails: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  openModel: boolean
  setOpenModel: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export default function ConfirmAccessRequest(props: IProps) {

  const resetRequestPosted = () => {
    props.setReceivedUserDetails(false)
    props.setParentRequestPosted(false)
    props.setOpenModel(false)
  }

  const moreThanOneUserDisplay = () => {
    return <div>
      The following users have had their request {messageDisplay()}
      <List>
        {props.emails.map(e =>
          <ListItem>
            <ListItemText
              primary={e}
            />
          </ListItem>,
        )}
      </List>
    </div>
  }

  const singleUserDisplay = () => {
    return <div>
      {props.emails} has had their request {messageDisplay()}
    </div>
  }

  const messageDisplay = () => {
    switch (props.message.toLowerCase()) {
      case "granted" :
        return "approved"
      case "revert" :
        return "reverted"
      default :
        return "dismissed"
    }
  }

  return (
    <div className="flex-container">
      <div>
        <Box sx={style}>
          <Typography align="center" id="modal-modal-title" variant="h6" component="h2">
            User access request {messageDisplay()}
          </Typography>
          <br/>
          {props.emails.length > 1 ? moreThanOneUserDisplay() : singleUserDisplay()}
          <Button style={{float: 'right'}} onClick={resetRequestPosted}>back</Button>
        </Box>
      </div>
    </div>
  );
}
