import { useLocation, useNavigate} from "react-router-dom";
import React, {useEffect, useState} from 'react';
import CheckSession from './CheckSession'
import HomeNavbar from './HomeNavbar'
import { Navbar } from 'react-bootstrap';

const Home = () => {
    const navigate = useNavigate();
    const [loggedIn, setLoggedIn] = useState(null);

    CheckSession(setLoggedIn);

    useEffect(() => {
        if (loggedIn === "false") {
            navigate("/");
        }
    }, [loggedIn, navigate]);

    return (
        <div>
            <HomeNavbar/>
            <h1>{loggedIn === "true" ? "Welcome Back!" : null}</h1>
        </div>
    );
};

export default Home;