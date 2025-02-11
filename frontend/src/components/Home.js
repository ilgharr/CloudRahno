import { useLocation } from "react-router-dom";
import React, {useEffect, useState} from 'react';
import { useNavigate } from 'react-router-dom';
import CheckSession from './CheckSession'

const Home = () => {
    const navigate = useNavigate();
    const [loggedIn, setLoggedIn] = useState(null);

    CheckSession(setLoggedIn);

    useEffect(() => {
        if (loggedIn === "false") {
            navigate("/");
        }
    }, [loggedIn, navigate]);

fetch("/fetch-token", {
  method: "POST",
  credentials: "include",
})
  .then(response => response.text())
  .then(data => console.log(data))
  .catch(err => console.error(err));

    return (
        <div>
        <h1>{loggedIn === "true" ? "Welcome Back!" : null}</h1>
        </div>
    );
};

export default Home;