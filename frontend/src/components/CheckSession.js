import React, {useState, useEffect} from 'react'
import {useNavigate} from 'react-router-dom'

// browser does not allow access to HTTPOnly marked cookies
// a cookie is basically a key value pair sent within the Header

const CheckSession = (setLoggedIn) => {

    useEffect(() => {
        (async () => {
            try {
                const response = await fetch(`/check-session`, {
                    method: "GET",
                    credentials: "include"
                });
                if (response.ok) {
                    const result = await response.json();
                    if (result.isLoggedIn === "true") {
                        setLoggedIn("true");
                    } else {
                        setLoggedIn("false");
                    }
                } else {
                    console.error("Unexpected backend response:", response.status);
                    setLoggedIn("false");
                }
            } catch (e) {
                console.error("Error communicating with backend:", e);
                setLoggedIn("false");
            }
        })(); // REMEMBER TO INVOKE THE ASYNC WITH () AT THE END OF IT!!!!!
    }, [setLoggedIn]);

}

export default CheckSession;