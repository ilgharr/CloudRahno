import React, {memo} from 'react';
import { Navbar } from 'react-bootstrap';
import logo from '../assets/cloud_logo.png'

const HomeNavbar = memo(() => {
    const handleLogout = () => {
        window.location.href = '/logout';
    }

    return(
        <Navbar className="p-0 nav-bar" expand="lg">

            <Navbar.Brand className="m-0 p-0 h-100">
                <img src={logo} alt="Brand Logo" className="logo-image" />
            </Navbar.Brand>

<Navbar.Brand className="logo-name">
    <a href="https://github.com/ilgharr/CloudRahno" class="logo-link" target="_blank" rel="noopener noreferrer">
        <h1 className="logo-name-text">CloudRahno</h1>
    </a>
    <span class="moving-arrow">⬅</span>
    <span class="github-text">GitHub</span>
</Navbar.Brand>

            <button className="logout-button" onClick={handleLogout}>
                Logout
            </button>
        </Navbar>
    )
});

export default HomeNavbar;