import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom"
import LoadingScreen from "./LoadingScreen";

const Callback = () => {
    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        const params = new URLSearchParams(location.search);
        const code = params.get("code");
        const error = params.get("error");

        if(!code || error){
            console.warn("Error or missing code returned in callback:", error);
            navigate("/", { state: { error: "Authentication Failed" } }); // Redirect to home or error page
            return;
        }

        (async () => {
            try {
                const response = await fetch(`/api/callback?code=${code}`, {method: "GET", credentials: "include"});
                if (response.ok) {
                    navigate("/home");
                } else {
                    console.error("Failed to retrieve user ID and/or Refresh Token from server. Status:", response.status);
                    navigate("/", { state: { error: "Failed to Login." } });
                }
            } catch (e) {
                console.error("Error communicating with backend:", e);
                navigate("/", { state: { error: "Unexpected error occurred." } });
            }
        })();
    }, [location, navigate]);
    // even tho "navigate" doesnt change it is good to add it as a dependency
    // because if react happens to go wrong for some reason, we want this here for safety

    return (<LoadingScreen />);
};

export default Callback;