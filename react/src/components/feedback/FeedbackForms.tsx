import React from 'react';
import {Formik, Form, Field} from 'formik';
import {
  RadioGroup,
  FormControlLabel,
  Radio,
  FormControl,
  FormLabel,
  Button,
  Typography,
  TextField
} from '@mui/material';
import {Stack} from "@mui/material";
import Grid from "@mui/material/Grid";
import Link from "@mui/material/Link";
import axios, {AxiosResponse} from "axios";
import {useParams} from "react-router-dom";
import ApiClient from "../../services/ApiClient";

interface FeedbackData {
  feedbackType: string;
  aORbTest: string;
  question_1: string;
  question_2: string;
  question_3: string;
  question_4: string;
  question_5: string;
}

export function FeedbackForms() {
  const {feedbackType = ''} = useParams<{ feedbackType?: string }>();
  const {abVersion=''} = useParams<{ abVersion?: string }>();
  const [currentQuestion, setCurrentQuestion] = React.useState(1);
  const [error, setError] = React.useState(false);
  const [question1, setQuestion1] = React.useState('');
  const [question2, setQuestion2] = React.useState('');
  const [question3, setQuestion3] = React.useState('');
  const [question4, setQuestion4] = React.useState('');
  const [question5, setQuestion5] = React.useState('');
  const [errorText, setErrorText] = React.useState('');

  const questionOneForm = (
    <Formik
      initialValues={{question1: '', text: ''}}
      onSubmit={(values) => {
        if (values.question1 === '' || values.question1 === undefined) {
          setError(true);
        } else if (values.question1 === 'other') {
          if (values.text === '') {
            setError(true);
          } else {
            setQuestion1(values.text)
            setCurrentQuestion(2);
            setError(false);
          }
        } else {
          setQuestion1(values.question1)
          setCurrentQuestion(2);
          setError(false);
        }
      }}
    >
      {({handleChange, values}) => (
        <Form>
          <Typography>{"Question 1 of 5"}</Typography>
          <FormControl component="fieldset">
            <FormLabel component="legend">
              <Typography variant="h5" sx={{fontWeight: 'bold', color: '#111224'}}>
                How would you describe your role at Border-force ? :
                <div style={{display: 'inline-block', "color": "#DB0F24"}}>*</div>
              </Typography>
            </FormLabel>
            <Typography sx={{"color": "#DB0F24", fontWeight: 'bold'}}>
              {error ? "Please select your role at Border force" : ""}
            </Typography>
            <Stack sx={{
              ...(error && {
                borderLeft: '4px solid red',
                pl: 1,
                ml: -1
              })
            }}>
              <Field as={RadioGroup} name="question1" value={values.question1} onChange={handleChange}>
                <FormControlLabel value="National manager" control={<Radio/>} label="National manager"/>
                <FormControlLabel value="Regional manager" control={<Radio/>} label="Regional manager"/>
                <FormControlLabel value="Operational - Front line" control={<Radio/>}
                                  label="Operational - Front line (including Watch-House and Duty Managers)"/>
                <FormControlLabel value="Operational - Back line" control={<Radio/>}
                                  label="Operational - Back line (including Support, Planning, Rostering, Performance)"/>
                <FormControlLabel value="other" control={<Radio/>} label="Other"/>
                {values.question1 === 'other' && (
                  <Field as={TextField}
                         name="text"
                         value={values.text}
                         onChange={handleChange}
                  />
                )}
              </Field>
            </Stack>
            <br/>
            <Button type="submit"
                    sx={{float: "left", width: 'auto', maxWidth: '120px', padding: '6px 12px'}}
                    variant="outlined">Continue</Button>
          </FormControl>
        </Form>
      )}
    </Formik>
  );

  const questionTwoForm = (
    <Formik
      initialValues={{question2: ''}}
      onSubmit={(values) => {
        if (values.question2 === '' || values.question2 === undefined) {
          setError(true);
        } else {
          setQuestion2(values.question2)
          setCurrentQuestion(3);
          setError(false);
        }
      }}
    >
      {({handleChange, values}) => (
        <Form>
          <Typography>{"Question 2 of 5"}</Typography>
          <FormControl component="fieldset">
            <FormLabel component="legend">
              <Typography variant="h5" sx={{fontWeight: 'bold', color: '#111224'}}>
                Overall, what did you think of the quality of DRT ? :
                <div style={{display: 'inline-block', "color": "#DB0F24"}}>*</div>
              </Typography>
            </FormLabel>
            <Typography sx={{"color": "#DB0F24", fontWeight: 'bold'}}>
              {error ? "Please select an option" : ""}
            </Typography>
            <Stack sx={{
              ...(error && {
                borderLeft: '4px solid red',
                pl: 1,
                ml: -1
              })
            }}>
              <Field as={RadioGroup} name="question2" value={values.question2} onChange={handleChange}>
                <FormControlLabel value="Very good" control={<Radio/>} label="Very good"/>
                <FormControlLabel value="Good" control={<Radio/>} label="Good"/>
                <FormControlLabel value="Average" control={<Radio/>}
                                  label="Average"/>
                <FormControlLabel value="Bad" control={<Radio/>}
                                  label="Bad"/>
                <FormControlLabel value="Very bad" control={<Radio/>} label="Very bad"/>
              </Field>
            </Stack>
            <br/>
            <Button type="submit"
                    sx={{float: "left", width: 'auto', maxWidth: '120px', padding: '6px 12px'}}
                    variant="outlined">Continue</Button>
          </FormControl>
        </Form>
      )}
    </Formik>
  )

  const questionThreeForm = (
      <Formik
        initialValues={{question3: ''}}
        onSubmit={(values) => {
          if (values.question3 === undefined) {
            setQuestion3('')
          } else {
            setQuestion3(values.question3)
          }
          setCurrentQuestion(4);
          setError(false);

        }}
      >
      {({handleChange, values}) => (
        <Form>
          <Typography>{"Question 3 of 5"}</Typography>
          <FormControl component="fieldset">
            <FormLabel component="legend">
              <Typography variant="h5" sx={{fontWeight: 'bold', color: '#111224'}}>
                What did you like about DRT? (optional) :
              </Typography>
            </FormLabel>
            <Typography>If possible, please give examples</Typography>
            <Field
              as={TextField}
              name="question3"
              multiline
              rows={3}
              value={values.question3}
              onChange={handleChange}
              variant="outlined"
              fullWidth
              style={{height: '100px', width: '400px'}}
            />
            <br/>
            <Grid container>
              <Grid xs={3}>
                <Button type="submit"
                        sx={{
                          float: "left",
                          width: 'auto',
                          maxWidth: '120px',
                          padding: '6px 12px 6px 12px'
                        }}
                        variant="outlined">Continue</Button>
              </Grid>
              <Grid xs={9} sx={{float: "left", padding: '6px 12px'}}>
                <Link href="#"
                      onClick={() => handleEvent(4)}>Skip</Link>
              </Grid>
            </Grid>
          </FormControl>
        </Form>
      )}
    </Formik>
  )

  const questionFourForm = (
    <Formik
      initialValues={{question4: ''}}
      onSubmit={(values) => {
        if (values.question4 === undefined) {
          setQuestion4('')
        } else {
          setQuestion4(values.question4)
        }
          setCurrentQuestion(5);
          setError(false);
      }}
    >
      {({handleChange, values}) => (
        <Form>
          <Typography>{"Question 4 of 5"}</Typography>
          <FormControl component="fieldset">
            <FormLabel component="legend">
              <Typography variant="h5" sx={{fontWeight: 'bold', color: '#111224'}}>
                What did you think could be improved in DRT ? (optional) :
              </Typography>
            </FormLabel>
            <Typography>If possible, please give examples and provide suggestions</Typography>
            <Field
              as={TextField}
              name="question4"
              multiline
              rows={3}
              value={values.question4}
              onChange={handleChange}
              variant="outlined"
              fullWidth
              style={{height: '100px', width: '400px'}}
            />
            <br/>
            <Grid container>
              <Grid xs={2}>
                <Button type="submit"
                        sx={{
                          float: "left",
                          width: 'auto',
                          maxWidth: '120px',
                          padding: '6px 12px'
                        }}
                        variant="outlined">Continue</Button>
              </Grid>
              <Grid xs={10} sx={{float: "left", padding: '6px 12px'}}>
                <Link href="#"
                      onClick={() => handleEvent(5)}>Skip</Link>
              </Grid>
            </Grid>
          </FormControl>
        </Form>
      )}
    </Formik>
  )

  const questionFiveForm = (
    <Formik
      initialValues={{question5: '', text: ''}}
      onSubmit={(values) => {
        if (values.question5 === '' || values.question5 === undefined) {
          setError(true);
        } else {
          setQuestion5(values.question5)
          setError(false);

          const feedbackData: FeedbackData = {
            feedbackType: feedbackType,
            aORbTest: abVersion,
            question_1: question1,
            question_2: question2,
            question_3: question3,
            question_4: question4,
            question_5: question5,
          }
          const handleResponse = (response: AxiosResponse) => {
            if (response.status === 200) {
              response.data
              setCurrentQuestion(6)
            } else {
              setCurrentQuestion(5)
              response.data
            }
          }

          axios.post(ApiClient.feedBacksEndpoint, feedbackData)
            .then(response => handleResponse(response))
            .then(data => {
              console.log(data);
            })
            .catch(error => {
              setErrorText('There was a problem saving the feedback data. Please try again later.');
              console.error(error);
            });

          console.log(feedbackData)
        }
      }}
    >
      {({handleChange, values}) => (
        <Form>
          <Typography>{"Question 5 of 5"}</Typography>
          <FormControl component="fieldset">
            <FormLabel component="legend">
              <Typography variant="h5" sx={{fontWeight: 'bold', color: '#111224'}}>
                Would you be interested in participating in a workshop? (approx. 30 mins) <div
                style={{display: 'inline-block', "color": "#DB0F24"}}>*</div>
              </Typography>
            </FormLabel>
            <Typography><p>You can share ideas with other teams and [regional officer] to improve DRT.</p>
              <p>We'll send you an email to inform you about upcoming sessions.</p></Typography>
            <Typography sx={{color: '#DB0F24', fontWeight: 'bold'}}>
              {error ? "Please select if you would be interested in participating in a workshop" : ""}
            </Typography>
            <Stack sx={{
              ...(error && {
                borderLeft: '4px solid red',
                pl: 1,
                ml: -1
              })
            }}>
              <Field as={RadioGroup} name="question5" value={values.question5} onChange={handleChange}>
                <FormControlLabel value="Yes" control={<Radio/>} label="Yes"/>
                <FormControlLabel value="No" control={<Radio/>} label="No"/>
              </Field>
            </Stack>
            <br/>
            <Button type="submit"
                    sx={{float: "left", width: 'auto', maxWidth: '180px', padding: '6px 12px'}}
                    variant="outlined">Submit feedback</Button>
          </FormControl>
          <Typography sx={{fontWeight: 'bold', color: '#DB0F24'}}>
            {errorText ? "Error while sending feedback . Please try again in sometime" : ""}
          </Typography>
        </Form>
      )}
    </Formik>
  );

  const handleEvent = (questionValue: number) => {
    setCurrentQuestion(questionValue);
  }

  const closeFeedback = (
    <Stack>
      <Typography variant="h5" sx={{float: "centre", fontWeight: 'bold', color: '#111224'}}>
        Thank you for your feedback !
      </Typography>
      <br/>
      <Typography variant="h5" sx={{float: "centre", fontWeight: 'bold', color: '#111224'}}>
        You may now close this window.
      </Typography>
    </Stack>
  )

  const displayQuestion = () => {
    switch (currentQuestion) {
      case 1:
        return questionOneForm;
      case 2:
        return questionTwoForm;
      case 3:
        return questionThreeForm;
      case 4:
        return questionFourForm;
      case 5:
        return questionFiveForm;
      case 6:
        return closeFeedback;
    }
  };

  return (
    <Stack>
      <Typography variant="h2" sx={{
        fontWeight: 'bold',
        color: '#233E82'
      }}>DRT Feedback</Typography>
      {displayQuestion()}
    </Stack>
  );
}
