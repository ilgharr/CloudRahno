import React from "react";

const SimplePopup = ({ message, onClose }) => {
    return (
        <div className="popup-overlay">
            <div className="popup-container">
                <p>{message}</p>
                <button onClick={onClose}>Close</button>
            </div>
        </div>
    );
};

export default SimplePopup;