import React, {useEffect, useState} from 'react';
import { Navbar, Container } from 'react-bootstrap';
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

            <Container className="landing-page-info">
                <p>Simple and Secure Cloud Storage for Everything You Need!
                <br/>
                Access your files anywhere, anytime. Safe, fast, and hassle-free</p>
            </Container>

            <div className="features-usage">
                <Container className="features-usage-content">
                    <div className="feature">
                        <h2>Features</h2>
                        <ul>
                            <li>Upload</li>
                            <li>Download</li>
                            <li>Delete</li>
                        </ul>
                    </div>
                    <div className="usage">
                        <h2>Usage</h2>
                        <ul>
                            <li>10GB of storage</li>
                            <li>10MB Upload Size</li>
                            <li>Unlimited files</li>
                        </ul>
                    </div>
                </Container>
            </div>

        </div>
    );
};

export default Landing;