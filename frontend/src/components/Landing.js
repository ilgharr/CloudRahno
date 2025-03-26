import React, {useEffect, useState} from 'react';
import { Container, Row, Col } from 'react-bootstrap';
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
    }, [loggedIn, navigate]);

    return (
        <div className="landing-page">
            <LandingNavbar/>
            <Container fluid className="landing-content">
                <Row className="hero-section">
                    <Col md={12} className="text-center">
                        <h1 className="hero-title">Cloud Storage for all your needs!</h1>
                        <p className="hero-subtitle">Secure, fast, and reliable cloud storage solution</p>
                    </Col>
                </Row>

                <Row className="features-section g-0">
                    <Col xs={12} md={6} className="d-flex justify-content-end pe-2">
                        <div className="feature-card">
                            <div className="feature-content">
                                <h2>Features</h2>
                                <ul className="feature-list">
                                    <li><i className="fas fa-upload"></i> Upload</li>
                                    <li><i className="fas fa-download"></i> Download</li>
                                    <li><i className="fas fa-trash"></i> Delete</li>
                                </ul>
                            </div>
                        </div>
                    </Col>
                    <Col xs={12} md={6} className="d-flex justify-content-start ps-2">
                        <div className="feature-card">
                            <div className="feature-content">
                                <h2>Usage</h2>
                                <ul className="feature-list">
                                    <li><i className="fas fa-hdd"></i> 10GB of storage</li>
                                    <li><i className="fas fa-file-upload"></i> 10MB Upload Size</li>
                                    <li><i className="fas fa-infinity"></i> Unlimited files</li>
                                </ul>
                            </div>
                        </div>
                    </Col>
                </Row>
            </Container>
        </div>
    );
};

export default Landing;