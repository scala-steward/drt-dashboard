import React, {useState} from "react";
import {Breadcrumbs, Button, Stack} from "@mui/material";
import {MarkdownEditor} from "./MarkdownEditor";
import {PreviewComponent} from "./PreviewComponent";
import axios, {AxiosResponse} from "axios";
import {Link} from "react-router-dom";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";

interface Props {
  id: string | undefined;
  videoURL: string;
  title: string | undefined;
  markdownContent: string | undefined;
  setReceivedData: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
  showEdit: boolean;
  setShowEdit: ((value: (((prevState: boolean) => boolean) | boolean)) => void);
}

export function FeatureGuideEdit(props: Props) {
  const [editTitle, setEditTitle] = React.useState(props.title)
  const [editMarkdownContent, setEditMarkdownContent] = React.useState(props.markdownContent)
  const [openPreview, setOpenPreview] = React.useState(false)
  const [updated, setUpdated] = useState(false);
  const [error, setError] = useState(false);

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setEditTitle(event.target.value);
  };

  const handleMarkdownChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    setEditMarkdownContent(event.target.value)
  }

  const handlePreviewOpen = () => {
    setOpenPreview(true);
    console.log("preview")
  }

  const handleBackToList = () => {
    props.setReceivedData(false);
    props.setShowEdit(false);
  }

  const handleResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      setUpdated(true);
      response.data
    } else {
      setError(true);
      response.data
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const formData = new FormData();
    formData.append('title', editTitle ? editTitle : '');
    formData.append('markdownContent', editMarkdownContent ? editMarkdownContent : '');
    axios.post('/guide/updateFeatureGuide/' + props.id, formData)
      .then(response => handleResponse(response))
      .then(data => {
        console.log(data);
      })
      .catch(error => {
        setError(true);
        console.error(error);
      });
  };

  return (
    error ? <div style={{marginTop: '20px', color: 'red'}}> There was a problem saving the feature guide <br/>
        <Button variant="outlined" color="primary" style={{marginTop: '20px'}}
                onClick={() => setError(false)}>Back</Button>
      </div> :
      updated ? <div> Feature update <br/>
          <Button variant="outlined" color="primary" style={{marginTop: '20px'}}
                  onClick={handleBackToList}>Upload another Feature update</Button></div> :
        <Stack sx={{mt: 2, gap: 4, alignItems: 'stretch'}}>
          <Breadcrumbs>
            <Link to={"/"}>
              Home
            </Link>
            <Link to={"/feature-guide-upload"} onClick={event => {
              event.preventDefault()
              props.setShowEdit(false)
            }}>
              Feature guides
            </Link>
            <Typography color="text.primary">Edit feature guide :: {editTitle}</Typography>
          </Breadcrumbs>
          <form onSubmit={handleSubmit}>
            <div style={{marginTop: '20px'}}>
              <TextField id="text"
                         label="Title"
                         value={editTitle}
                         onChange={handleInputChange}/>
            </div>
            <div style={{marginTop: '20px', font: "Arial", fontSize: "16px"}}>
              <label htmlFor="markdown">Markdown</label>
              <MarkdownEditor
                markdownContent={editMarkdownContent && editMarkdownContent?.length > 0 ? editMarkdownContent : ""}
                handleMarkdownChange={handleMarkdownChange}
              />
            </div>
            <Button variant="outlined" color="primary" type="submit">Save changes</Button>
          </form>
          {editMarkdownContent && editMarkdownContent.length > 0 ?
            <Button variant="contained" color="primary" onClick={handlePreviewOpen}>Preview</Button> :
            <span/>
          }

          <PreviewComponent title={editTitle}
                            videoURL={props.videoURL}
                            markdownContent={editMarkdownContent}
                            openPreview={openPreview}
                            setOpenPreview={setOpenPreview}/>

        </Stack>
  )
}
