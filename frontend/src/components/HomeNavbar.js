import React, {useState} from 'react';
import { Navbar } from 'react-bootstrap';
import logo from '../assets/cloud_logo.png'

const HomeNavbar =() => {
    const handleLogout = () => {
        window.location.href = '/logout';
    }

    return(
        <Navbar className="p-0 nav-bar" expand="lg">

            <Navbar.Brand className="m-0 p-0 h-100">
                <img src={logo} alt="Brand Logo" className="logo-image" />
            </Navbar.Brand>

            <Navbar.Brand  className="logo-name">
               <h1 className="logo-name-text">CloudRahno</h1>
            </Navbar.Brand>

            <button className="logout-button" onClick={handleLogout}>
                Logout
            </button>
        </Navbar>
    )
}

export default HomeNavbar;