import React, {useState} from 'react';
import { Navbar, Container, Button } from 'react-bootstrap';

import logo from '../assets/logoCloud1CutLarge.png'

const LandingNavbar =() => {
    const handleLogin = () => {
        window.location.href = '/login';
    }

    return(
        <Navbar className="p-0 nav-bar" expand="lg">

            <Navbar.Brand className="m-0 p-0 h-100">
                <img src={logo} alt="Brand Logo" className="logo-image" />
            </Navbar.Brand>

            <Navbar.Brand  className="logo-name">
               <h1 className="logo-name-text">CloudRahno</h1>
            </Navbar.Brand>

            <Button className="login-button" onClick={handleLogin}>
                Get Started
            </Button>

        </Navbar>
    )
}

export default LandingNavbar;