import React, {useEffect, useState} from 'react';
import { Navbar, Container, Button } from 'react-bootstrap';
import LandingNavbar from './LandingNavbar'
import { useNavigate } from 'react-router-dom';
import CheckSession from './CheckSession'

const Landing = () => {
    const navigate = useNavigate();
    const [loggedIn, setLoggedIn] = useState(null);

    CheckSession(setLoggedIn);

    useEffect(() => {
        if(loggedIn === "true"){
            navigate("/home");
        }
    }, [loggedIn, navigate])

    return (
        <div>
            <LandingNavbar/>
            <div className="landing-page-info">
                <p>Simple and Secure Cloud Storage for Everything You Need!</p>
                <p>Access your files anywhere, anytime. Safe, fast, and hassle-free</p>
            </div>
            <div className="features-usage">
                  <div className="features-usage-content">
                    <h2>Features and Usage</h2>
                    <p>Upload, download, and delete files</p>
                    <p>10GB of storage with a max upload size of 10MB</p>
                  </div>
            </div>
                <div className="notice">
                      <h3>Important Notice</h3>
                      <p>
                          This website is not open to the public. Hosting file storage comes with significant costs,
                          and the creator of this website cannot compete with larger companies.
                          Additionally, there are many legal considerations associated with storing files online.
                      </p>
                      <p>
                          Recruiters may request a temporary email and password. However,
                          please be aware that all files uploaded will be visible to the developer.
                      </p>
                  </div>
        </div>
    );
};

export default Landing;