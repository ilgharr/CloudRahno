import { useNavigate } from "react-router-dom";
import React, { useEffect, useState, useRef } from "react";
import { Container } from "react-bootstrap";
import CheckSession from "./CheckSession";
import HomeNavbar from "./HomeNavbar";
import UploadCloud from "../assets/cloud_upload.png";

const handleFileUpload = async (file) => {
    if (!file || file.length === 0) {
        return;
    }

    const formData = new FormData();
    file.forEach((f) => {
        formData.append("file", f);
    });

    try {
        const response = await fetch(`/upload`, {
            method: "POST",
            credentials: "include",
            body: formData
        });

        if (response.ok) {
            const result = await response.text();
        } else {
            const errorMessage = await response.text();
            console.error("Error:", errorMessage);
        }
    } catch (error) {
        console.error("Error during file upload:", error);
    }
};

const Home = () => {
    const navigate = useNavigate();
    const fileInput = useRef(null);
    const [loggedIn, setLoggedIn] = useState(null);
    const [file, setFile] = useState([]);
    const [zipName, setZipName] = useState("");
    const [fileSize, setFileSize] = useState(0);
    const MAX_SIZE = 10 * 1024 * 1024;

    CheckSession(setLoggedIn);

    useEffect(() => {
        if (loggedIn === "false") {
            navigate("/");
        }
    }, [loggedIn, navigate]);

    const handleClick = () => {
        fileInput.current.click();
    };

    const handleFileChange = (event) => {
        const selectedFiles = Array.from(event.target.files);
        setFile(selectedFiles);
        const size = selectedFiles.reduce((acc, file) => acc + file.size, 0);
        setFileSize(size);
    };

    const handleFileRemove = () => {
        setFile([]);
        setFileSize(0);
    };

    const handleFileSubmit = async () => {
        await handleFileUpload(file);
        setFile([]);
        setFileSize(0);
    };


    return (
        <div>
            <HomeNavbar />
            <Container>
                {file.length < 1 ? (
                    <Container className="upload-file" onClick={handleClick}>
                        <p>Drag and Drop Or Click to Select Files</p>
                        <img
                            src={UploadCloud}
                            alt="Upload File Image"
                            className="upload-file-image"
                        />
                        <input
                            type="file"
                            ref={fileInput}
                            style={{ display: "none" }}
                            onChange={handleFileChange}
                            multiple
                        />
                    </Container>
                ) : (
                    <>
                        {fileSize < MAX_SIZE ? (
                            <>
                                <Container className="d-flex flex-row justify-content-center align-items-center main-submission">
                                    <p style={{paddingTop: "8px"}}>{file.length} file(s) selected</p>
                                    <button onClick={handleFileSubmit}>Submit</button>
                                    <button onClick={handleFileRemove}>Remove</button>
                                </Container>
                            </>
                        ) : (
                            <>
                                {alert("No file larger than 10 MB")}
                                {handleFileRemove()}
                            </>
                        )}
                    </>
                )}
            </Container>
        </div>
    );
};

export default Home;