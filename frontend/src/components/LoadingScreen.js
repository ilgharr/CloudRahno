import React, { useEffect } from "react";
import earth from "../assets/earth_rotating.png"
const LoadingScreen = () => {
  return (
    <div className="loading-screen">
      <div className="earth-container">
        <img
          src={earth}
          alt="Rotating Earth"
          className="rotating-earth"
        />
      </div>
      <div className="loading-text">Loading...</div>
    </div>
  );
};

export default LoadingScreen;