import React from 'react';
import './App.css';
import Home from './components/Home';

function App() {
  const url = window.document.location;
  const logoutLink = "/oauth/logout?redirect=" + url
  
  return (
    <div className="App">
      <header role="banner" id="global-header" className=" with-proposition">
        <div className="header-wrapper">
          <div className="header-global">
            <div className="header-logo">
              <a href="https://www.gov.uk" title="Go to the GOV.UK homepage" id="global-header-logo"
                 className="content">
                <img
                  src="images/gov.uk_logotype_crown_invert_trans.png"
                  width="36" height="32" alt=""/> GOV.UK
              </a>
            </div>
          </div>
          <div className="header-proposition">
            <div className="logout">
              <a href={logoutLink} id="proposition-name">Log out</a>
            </div>
            <div className="content">
              <a href="/" id="proposition-name">Dynamic Response Tool</a>
            </div>
          </div>
        </div>
      </header>
      <div id="global-header-bar"></div>
      <Home/>
      <footer className="group js-footer" id="footer" role="contentinfo">
        <div className="footer-wrapper">
          <div className="footer-meta">
            <div className="footer-meta-inner">

              {/*<div className="open-government-licence">*/}
              {/*  <p className="logo"><a href="https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/"*/}
              {/*                         rel="license">Open Government Licence</a></p>*/}

              {/*  <p>All content is available under the <a*/}
              {/*    href="https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/" rel="license">Open*/}
              {/*    Government Licence v3.0</a>, except where otherwise stated</p>*/}

              {/*</div>*/}
            </div>

            {/*<div className="copyright">*/}
            {/*  <a*/}
            {/*    href="http://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/">©*/}
            {/*    Crown copyright</a>*/}
            {/*</div>*/}
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;
