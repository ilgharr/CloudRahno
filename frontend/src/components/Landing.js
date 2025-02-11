import React, {useEffect, useState} from 'react';
import { Navbar, Container, Button } from 'react-bootstrap';
import LandingNavbar from './LandingNavbar'
import Footer from './Footer'
import "./CssHelper.js"
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
                    <p>"Simple and Secure Cloud Storage for Everything You Need!"</p>
                    <p>"Access your files anywhere, anytime. Safe, fast, and hassle-free."</p>
                </Container>
            <Footer/>
        </div>
    );
};

export default Landing;