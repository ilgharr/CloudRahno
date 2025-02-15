import 'bootstrap/dist/css/bootstrap.min.css';
import React, {useEffect} from 'react';
import { Routes, Route } from 'react-router-dom';
import { useCookies } from "react-cookie";
import Home from './components/Home'
import Landing from './components/Landing'
import Callback from './components/Callback'
import "./components/CssHelper.js"
import Footer from './components/Footer'

const App = () => {
    const [cookies, setCookie] = useCookies(["userId"]);

    useEffect(() => {
        if (!cookies.userId){
            setCookie("userId", null, {path:"/", maxAge: 432000})
        }
    }, [cookies, setCookie])
    return (
        <div>

        <Routes>
            <Route path="/" element={<Landing />} />
            <Route path="/home" element={<Home />} />
            <Route path="/callback" element={<Callback />} />
        </Routes>
        <Footer/>
        </div>
    );
};

export default App;