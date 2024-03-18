import React, {useEffect, useState} from 'react'
import {MarkdownEditor} from './MarkdownEditor'
import axios from "axios"
import {PreviewComponent} from "./PreviewComponent"
import Button from "@mui/material/Button"
import {Breadcrumbs, Stack} from "@mui/material"
import {Link, useNavigate, useParams} from "react-router-dom"
import Typography from "@mui/material/Typography"
import {enqueueSnackbar} from "notistack"
import {FeatureGuide} from "./FeatureGuidesList"
import TextField from "@mui/material/TextField"
import ApiClient from "../../services/ApiClient";

export const AddOrEditFeatureGuide = () => {
  const navigate = useNavigate()

  const [file, setFile] = useState<File | null>(null)
  const [title, setTitle] = useState('')
  const [markdownContent, setMarkdownContent] = useState('')
  const [openPreview, setOpenPreview] = useState(false)
  const [fileName, setFileName] = useState('')
  const params = useParams()
  const guideId = params['guideId'] ? params['guideId'] : ''

  if (guideId) {
    useEffect(() => {
      const fetch = () => axios.get(`${ApiClient.featureGuidesEndpoint}/${guideId}`)
        .then(response => {
          if (response.status === 200) {
            const guide = response.data as FeatureGuide
            setTitle(guide.title)
            setMarkdownContent(guide.markdownContent)
            setFileName(guide.fileName)
          } else {
            enqueueSnackbar('Failed to get feature guides', {variant: 'error'})
          }
        })
        .catch(() =>
          enqueueSnackbar('Failed to get feature guides', {variant: 'error'})
        )
      fetch()
    }, [])
  }

  const handlePreviewOpen = () => {
    // video && setVideoUrl(URL.createObjectURL(video))
    setOpenPreview(true)
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setFile(e.target.files[0])
    }
  }

  const fileUrl = file ? URL.createObjectURL(file) : `${ApiClient.featureGuidesFileEndpoint}/${fileName}`
  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    console.log(`submitting form data: ${title}, ${markdownContent}`)

    const formData = new FormData()
    file && formData.append('featureFile', file)
    formData.append('title', title)
    formData.append('markdownContent', markdownContent)
    const doAction = guideId ?
      axios.put(`${ApiClient.featureGuidesEndpoint}/${guideId}`, formData) :
      axios.post(ApiClient.featureGuidesEndpoint, formData)

    doAction
      .then(response => {
          if (response.status === 200) {
            enqueueSnackbar('Feature guide uploaded successfully', {variant: 'success'})
            navigate('/feature-guides')
          } else {
            enqueueSnackbar('Feature guide upload failed', {variant: 'error'})
          }
        }
      )
      .catch(() => enqueueSnackbar('Feature guide upload failed', {variant: 'error'}))
  }

  return <Stack gap={4} alignItems={'stretch'} sx={{mt: 2}}>
    <Breadcrumbs>
      <Link to={"/"}>
        Home
      </Link>
      <Link to={"/feature-guides"}>
        Feature guides
      </Link>
      <Typography color="text.primary">{guideId ? 'Edit' : 'Add'}</Typography>
    </Breadcrumbs>
    <form onSubmit={handleSubmit}>
      {!guideId && <Stack sx={{gap: 1}}>
          <div><label htmlFor="image">Demo video (webm format) / Image</label></div>
          <input type={'file'} id="featureFile" onChange={handleFileChange}/>
      </Stack>}
      <div style={{marginTop: '20px'}}>
        <TextField label={'Title'} value={title} onChange={event => setTitle(event.target.value)}/>
      </div>
      <div style={{marginTop: '20px', font: "Arial", fontSize: "16px"}}>
        <label htmlFor="markdown">Markdown:</label>
        <MarkdownEditor
          markdownContent={markdownContent}
          handleMarkdownChange={event => setMarkdownContent(event.target.value)}
        />
      </div>
      <Stack gap={1} direction={'row'}>
        <Button variant={"outlined"} onClick={() => navigate('/feature-guides')}>Cancel</Button>
        <Button variant={"outlined"} color={"primary"} type={"submit"}>{guideId ? 'Save changes' : 'Save'}</Button>
      </Stack>
    </form>
    {markdownContent.length > 0 ?
      <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center'}}>
        <Button variant="contained" color="primary" onClick={handlePreviewOpen}>Preview</Button>
      </div> : <span/>}

    {openPreview &&
        <PreviewComponent
            guide={{
              id: '',
              title: title,
              markdownContent: markdownContent,
              fileName: '',
              uploadTime: ''
            } as FeatureGuide}
            fileUrl={fileUrl}
            onClose={() => setOpenPreview(false)}
            actionsAvailable={false}
        />}
  </Stack>
}
