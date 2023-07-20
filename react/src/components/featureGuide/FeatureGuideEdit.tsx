import React, {useState} from "react";
import {Button} from "@mui/material";
import {MarkdownEditor} from "./MarkdownEditor";
import {PreviewComponent} from "./PreviewComponent";
import axios, {AxiosResponse} from "axios";

interface Props {
  id: string | undefined;
  videoURL: string | undefined;
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

  const handleBack = () => {
    props.setShowEdit(false);
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
    formData.append('title', editTitle);
    formData.append('markdownContent', editMarkdownContent);
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
        <div>
          <form onSubmit={handleSubmit}>
            <h1>Edit Feature Guide</h1>
            <div style={{marginTop: '20px'}}>
              <label htmlFor="text">Title </label>
              <input id="text" value={editTitle} onChange={handleInputChange}/>
            </div>
            <div style={{marginTop: '20px', font: "Arial", fontSize: "16px"}}>
              <label htmlFor="markdown">Markdown:</label>
              <MarkdownEditor
                markdownContent={editMarkdownContent && editMarkdownContent?.length > 0 ? editMarkdownContent : ""}
                handleMarkdownChange={handleMarkdownChange}
              />
            </div>
            <div style={{marginLeft: '100px'}}>
              <Button variant="outlined" color="primary" type="submit">Update</Button>
            </div>
          </form>
          {editMarkdownContent && editMarkdownContent.length > 0 ?
            <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
              <Button variant="contained" color="primary" onClick={handlePreviewOpen}>Preview</Button>
            </div> : <span/>}

          <PreviewComponent title={editTitle} videoURL={props.videoURL}
                            markdownContent={editMarkdownContent} openPreview={openPreview}
                            setOpenPreview={setOpenPreview}/>

          <Button style={{float: 'right'}} variant="outlined"
                  color="primary"
                  onClick={handleBack}>back</Button>
        </div>
  )
}
