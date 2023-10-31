import React, {useEffect, useState} from 'react';
import {MarkdownEditor} from './MarkdownEditor';
import axios, {AxiosResponse} from "axios";
import ListFeatureGuide from "./ListFeatureGuide";
import {PreviewComponent} from "./PreviewComponent";
import Button from "@mui/material/Button";

const UploadForm: React.FC = () => {
  const [video, setVideo] = useState<File | null>(null);
  const [text, setText] = useState('');
  const [markdownContent, setMarkdownContent] = useState('');
  const [uploaded, setUploaded] = useState(false);
  const [error, setError] = useState(false);
  const [openPreview, setOpenPreview] = useState(false);
  const [viewFeatureGuides, setViewFeatureGuides] = useState(false);
  const [videoUrl, setVideoUrl] = useState('');

  const handlePreviewOpen = () => {
    setVideoUrl(URL.createObjectURL(video));
  }

  useEffect(() => {
    if (videoUrl !== '') {
      setOpenPreview(true);
    }
  }, [videoUrl]);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setVideo(e.target.files[0]);
    }
  };

  const handleInputChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setText(event.target.value);
  };

  const handleMarkdownChange = (event: React.ChangeEvent<HTMLTextAreaElement>) => {
    setMarkdownContent(event.target.value);
  };

  const handleResponse = (response: AxiosResponse) => {
    if (response.status === 200) {
      setUploaded(true);
      response.data
    } else {
      setError(true);
      response.data
    }
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const formData = new FormData();
    video && formData.append('webmFile', video);
    formData.append('title', text);
    formData.append('markdownContent', markdownContent);
    axios.post('/guide/uploadFeatureGuide', formData)
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
      uploaded ? <div style={{marginTop: '20px'}}> New feature guide successfully saved <br/>
          <Button variant="outlined" color="primary" style={{marginTop: '20px'}}
                  onClick={() => setUploaded(false)}>Upload another file</Button>
        </div> :
        viewFeatureGuides ? <ListFeatureGuide setViewFeatureGuides={setViewFeatureGuides}/> :
          <div>
            <form onSubmit={handleSubmit}>
              <h1>New Feature Guide | <a href="#" style={{marginTop: '20px'}}
                                         onClick={() => setViewFeatureGuides(true)}>View Feature list</a>
              </h1>
              <div style={{marginTop: '20px'}}>
                <label htmlFor="image">Feature demo video (webm format) : </label>
                <input type="file" id="webmFile" accept="File/*" onChange={handleImageChange}/>
              </div>
              <div style={{marginTop: '20px'}}>
                <label htmlFor="text">Title </label>
                <input id="text" value={text} onChange={handleInputChange}/>
              </div>
              <div style={{marginTop: '20px', font: "Arial", fontSize: "16px"}}>
                <label htmlFor="markdown">Markdown:</label>
                <MarkdownEditor
                  markdownContent={markdownContent}
                  handleMarkdownChange={handleMarkdownChange}
                />
              </div>
              <div style={{marginLeft: '100px'}}>
                <Button variant="outlined" color="primary" type="submit">Upload</Button>
              </div>
            </form>
            {markdownContent.length > 0 ?
              <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
                <Button variant="contained" color="primary" onClick={handlePreviewOpen}>Preview</Button>
              </div> : <span/>}

            <PreviewComponent videoURL={video ? videoUrl : ""}
                              title={text}
                              markdownContent={markdownContent}
                              openPreview={openPreview}
                              setOpenPreview={setOpenPreview}/>
          </div>

  );
};

export default UploadForm;
