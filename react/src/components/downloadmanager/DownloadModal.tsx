import * as React from 'react';
import {Button, CircularProgress, Dialog, DialogContent, DialogTitle, Typography} from "@mui/material";
import Link from "@mui/material/Link";


interface DownloadModalProps {
  status: string;
  downloadUrl: string;
  isModalOpen: boolean;
  handleModalClose: () => void;
}

const DownloadModal = ({status, downloadUrl, isModalOpen, handleModalClose}: DownloadModalProps) => {
  return (
          <Dialog
            open={isModalOpen}
            onClose={() => handleModalClose()}
          >
          <DialogTitle sx={{textAlign: 'center'}}>Your report is being created</DialogTitle>
          <DialogContent sx={{ display: 'flex', justifyContent: 'center', flexDirection: 'column', textAlign: 'center'}}>
            {status === 'preparing' && <CircularProgress sx={{margin: '0 auto 1em'}} />}
            <Typography>The time required to crete the report varies based on its size, this could up to 10 minutes. Once the report is ready, we will send you an email containing a download link. Alternatively, you can choose to wait and download the report directly from this page after it's finished. Closing this window will not effect creation of your report.</Typography>
            <Button sx={{margin: '1em auto'}} component={Link} target={'_blank'} href={downloadUrl} disabled={status !== 'complete'} color='primary'>Download</Button>
          </DialogContent>
          </Dialog>
  )
}

export default DownloadModal;
