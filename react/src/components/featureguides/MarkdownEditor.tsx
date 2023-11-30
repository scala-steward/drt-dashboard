import React from 'react';
import ReactMarkdown from 'react-markdown';
import {Box} from "@mui/material";

export const MarkdownEditor = ({markdownContent, handleMarkdownChange}: {
  markdownContent: string,
  handleMarkdownChange: (event: React.ChangeEvent<HTMLTextAreaElement>) => void
}) => {
  return (
    <Box sx={{display: "flex", font: "Arial", fontSize: "16px"}}>
      <Box sx={{flex: 1}}>
                <textarea
                  value={markdownContent}
                  onChange={handleMarkdownChange}
                  placeholder="Enter Markdown content"
                  style={{height: '300px', width: '300px'}}/>
      </Box>
      <Box sx={{flex: "1", marginLeft: "5px"}}>
        <ReactMarkdown>{markdownContent}</ReactMarkdown>
      </Box>
    </Box>
  );
};
