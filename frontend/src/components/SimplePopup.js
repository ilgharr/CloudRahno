import React from "react";

const SimplePopup = ({ message, onClose }) => {
    return (
        <div className="popup-overlay">
            <div className="popup-container">
                <div dangerouslySetInnerHTML={{ __html: message }} />
                <button onClick={onClose}>Close</button>
            </div>
        </div>
    );
};

export default SimplePopup;