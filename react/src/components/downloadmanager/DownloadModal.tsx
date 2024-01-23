import * as React from 'react';
import {Button, CircularProgress, Dialog, DialogContent, DialogTitle, Typography, IconButton} from "@mui/material";
import CloseIcon from '@mui/icons-material/Close';
import Link from "@mui/material/Link";


interface DownloadModalProps {
  status: string;
  downloadUrl: string;
  isModalOpen: boolean;
  handleModalClose: () => void;
}

const DownloadModal = ({status, downloadUrl, isModalOpen, handleModalClose}: DownloadModalProps) => {

  let titleText, paragraphText, buttonLabel;
  switch (status) {
    case 'preparing':
      titleText = "Your report is being created"
      paragraphText = "The time required to create the report varies based on its size, this could be up to 10 minutes. Once the report is ready, we will send you an email containing a download link. Alternatively, you can choose to wait and download the report directly from this page after it's finished. Closing this window will not effect creation of your report."
      buttonLabel = "Download pending..."
      break;
    case 'complete':
      titleText = "Your report is ready"
      paragraphText = "Click the download button below to download your report. We have also sent you an email containing a link to download your report."
      buttonLabel = "Download report"
      break;
    case 'failed':
      titleText = "There was a problem"
      paragraphText = "Unfortunately there was a problem creating the report you requested. Please accept our apologies. Feel free to try again. If the failure continues to happen then please contact the DRT team at drtpoiseteam@homeoffice.gov.uk"
      break;
  }

  return (
          <Dialog
            open={isModalOpen}
            onClose={() => handleModalClose()}
          >
            <DialogTitle sx={{textAlign: 'center'}}>{ titleText }</DialogTitle>
            <IconButton
              aria-label="close"
              onClick={handleModalClose}
              sx={{
                position: 'absolute',
                right: 10,
                top: 10,
              }}
            >
              <CloseIcon />
            </IconButton>
            <DialogContent sx={{ display: 'flex', justifyContent: 'center', flexDirection: 'column', textAlign: 'center'}}>
              { status === 'preparing' && <CircularProgress sx={{margin: '0 auto 1em'}} /> }
              <Typography>{ paragraphText }</Typography>
              { status !== 'failed' && <Button 
                                        sx={{margin: '1em auto'}} 
                                        component={Link} 
                                        target={'_blank'} 
                                        href={downloadUrl} 
                                        disabled={status !== 'complete'} 
                                        color='primary'>
                                          { buttonLabel }
                                        </Button> }
            </DialogContent>
          </Dialog>
  )
}

export default DownloadModal;
