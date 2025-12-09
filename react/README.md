# DRT Dashboard

## Overview
This codebase contains both a Scala backend and a React frontend for the DRT Dashboard application. The frontend is
built using Create React App and communicates with the Scala backend via RESTful APIs.


## Scala Backend
To run the backend enter the root of the codebase and run:

USE_PG_SSL=false \
USE_PG_SSL_MODE=require \
AWS_ACCESS_KEY_ID=xx \
AWS_SECRET_ACCESS_KEY=xx \
AWS_S3_BUCKET=drt-local \
TEAM_EMAIL=x@x \
ACCESS_REQUEST_EMAIL=y@y \
DRT_DOMAIN=drt.localhost \
GOV_NOTIFY_API_KEY=xx \
KEY_CLOAK_API_URL= \
KEY_CLOAK_TOKEN_URL=https://localhost \
KEY_CLOAK_CLIENT_ID=drt-acp \
KEY_CLOAK_CLIENT_SECRET= \
KEY_CLOAK_USERNAME= \
KEY_CLOAK_PASSWORD= \
NO_JSON_LOGGING= \
ENABLED_PORTS=stn,lcy,ltn,nwi,sen,bhx,ema,lhr \
SLACK_WEBHOOK_URL= \
sbt run 


## React Frontend

Navigate to the `react` directory and install dependencies:

### `npm ci`

Then start the app:

### `npm start`

Runs the app in the development mode.<br />
Open [http://localhost:3000](http://localhost:3000) to view it in the browser.

The page will reload if you make edits.<br />
You will also see any lint errors in the console.

### `npm run test`

Launches the test runner in the interactive watch mode.<br />
See the section about [running tests](https://facebook.github.io/create-react-app/docs/running-tests) for more information.

### `npm run build`

Builds the app for production to the `build` folder.<br />
It correctly bundles React in production mode and optimizes the build for the best performance.

The build is minified and the filenames include the hashes.<br />
Your app is ready to be deployed!

See the section about [deployment](https://facebook.github.io/create-react-app/docs/deployment) for more information.

