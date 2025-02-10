import { useLocation } from "react-router-dom";
import React, {useEffect, useState} from 'react';
import { useNavigate } from 'react-router-dom';
import CheckSession from './CheckSession'

const Home = () => {
    const navigate = useNavigate();
    const [loggedIn, setLoggedIn] = useState("");

    CheckSession(setLoggedIn);

    return (
        <div>
        <h1>{loggedIn === "true" ? "Welcome Back!" : "Please log in."}</h1>
        </div>
    );
};

export default Home;