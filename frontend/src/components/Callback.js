import { useEffect } from "react";
import { useLocation, useNavigate } from "react-router-dom"

// file name is encoded for safety
// certain characters may cause issues
const callbackApi = async (code) => {
    try {
        const response = await fetch(`/api/callback?code=${code}`, {
            method: "GET",
            credentials: "include"
        });
        if (response.ok) {
            return true;
        } else {
            console.error("Failed to retrieve user ID and/or Refresh Token from server. Status:", response.status);
            return false;
        }
    } catch (e) {
        console.error("Error communicating with backend:", e);
        return false;
    }
};

const Callback = () => {
    const location = useLocation();
    const navigate = useNavigate();

    useEffect(() => {
        const executeTasks = async () => {
            const params = new URLSearchParams(location.search);
            const code = params.get("code");
            const error = params.get("error");

            if (!code || error) {
                console.warn("Error or missing code returned in callback:", error);
                navigate("/", { state: { error: "Authentication Failed" } });
                return;
            }

            const callbackStatus = await callbackApi(code);
            if (!callbackStatus) {
                navigate("/", { state: { error: "Failed to validate callback." } });
                return;
            }

            navigate("/home");
        };

        executeTasks();
    }, [location, navigate]);

    return null;
};

export default Callback;