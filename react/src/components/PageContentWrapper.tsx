import React from 'react';
import Stack from '@mui/material/Stack';

interface PageContentWrapperProps {
  children?: React.ReactNode;
}

const TabPanel = ({ children }: PageContentWrapperProps) => {
  return <Stack gap={4} alignItems={'stretch'} sx={{mt: 2, mb: 10}}>{ children }</Stack>;
}

export default TabPanel;

