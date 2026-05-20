import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
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
  border: '1px solid',
  borderColor: 'divider',
  borderRadius: 1,
  boxShadow: 24,
  p: 4,
};

interface IProps {
  emails: string[]
  failedEmails?: string[]
  message: string
  parentRequestPosted: boolean
  setParentRequestPosted: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  receivedUserDetails: boolean
  setReceivedUserDetails: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  openModel: boolean
  setOpenModel: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export default function ConfirmAccessRequest(props: IProps) {
  const successfulEmails = props.emails
  const failedEmails = props.failedEmails ?? []
  const hasSuccesses = successfulEmails.length > 0
  const hasFailures = failedEmails.length > 0

  const resetRequestPosted = () => {
    props.setReceivedUserDetails(false)
    props.setParentRequestPosted(false)
    props.setOpenModel(false)
  }

  const emailList = (emails: string[]) => (
    <List dense disablePadding>
      {emails.map(e =>
        <ListItem key={e}>
          <ListItemText
            primaryTypographyProps={{variant: 'body2'}}
            primary={e}
          />
        </ListItem>,
      )}
    </List>
  )

  const actionLabel = () => {
    switch (props.message.toLowerCase()) {
      case "granted" :
        return "approved"
      case "revert" :
        return "reverted"
      default :
        return "dismissed"
    }
  }

  const succeededHeading = () => actionLabel().charAt(0).toUpperCase() + actionLabel().slice(1)

  const failedHeading = () => `Could not be ${actionLabel()}`

  const summaryDisplay = () => {
    if (hasSuccesses && hasFailures) {
      return 'Some requests completed successfully, but some still need attention.'
    }

    if (hasFailures) {
      return 'No selected requests were completed.'
    }

    return successfulEmails.length > 1
      ? 'The selected requests were completed successfully.'
      : 'The selected request was completed successfully.'
  }

  const helpText = () => {
    if (!hasFailures) {
      return null
    }

    return 'Please retry the failed users. If the issue persists, check the console logs or contact the DRT team for support.'
  }

  const titleDisplay = () => {
    if (hasSuccesses && hasFailures) {
      return `User access request partially ${actionLabel()}`
    }

    if (hasFailures) {
      return `User access request could not be ${actionLabel()}`
    }

    return `User access request ${actionLabel()}`
  }

  return (
    <div className="flex-container">
      <div>
        <Box sx={style}>
          <Stack spacing={2}>
            <Typography align="center" id="modal-modal-title" variant="h6" component="h2">
              {titleDisplay()}
            </Typography>

            <Typography variant="body2" color="text.secondary">
              {summaryDisplay()}
            </Typography>

            {hasSuccesses ? <Box>
              <Typography variant="subtitle2" sx={{mb: 1}}>
                {succeededHeading()}
              </Typography>
              {emailList(successfulEmails)}
            </Box> : null}

            {hasFailures ? <Box>
              <Typography variant="subtitle2" sx={{mb: 1}}>
                {failedHeading()}
              </Typography>
              {emailList(failedEmails)}
            </Box> : null}

            {helpText() ? <Typography variant="body2" color="text.secondary">
              {helpText()}
            </Typography> : null}

            <Box sx={{display: 'flex', justifyContent: 'flex-end'}}>
              <Button onClick={resetRequestPosted}>Back</Button>
            </Box>
          </Stack>
        </Box>
      </div>
    </div>
  );
}
