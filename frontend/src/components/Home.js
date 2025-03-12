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
        const response = await fetch("http://localhost:8443/upload", {
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

const handleFileDownload = async () => {
    try {
        const response = await fetch("http://localhost:8443/download", {
            method: "POST",
            credentials: "include"
        });

        if (!response.ok) {
            const errorMessage = await response.text();
            console.error("Error downloading files:", errorMessage);
        } else {
            console.log("Download request sent successfully.");
        }
    } catch (error) {
        console.error("Error during file download:", error);
    }
};

const parseMultipartFiles = (boundary, responseText) => {
    const files = [];
    const parts = responseText.split(`--${boundary}`); // Split the response into parts using the boundary
    for (const part of parts) {
        // Extract file headers and file content
        const headersEndIndex = part.indexOf("\r\n\r\n");
        if (headersEndIndex !== -1) {
            const headersText = part.slice(0, headersEndIndex).trim();
            const content = part.slice(headersEndIndex + 4).trim(); // File content
            const filenameMatch = headersText.match(/filename="(.+?)"/); // Extract filename from headers

            if (filenameMatch) {
                const filename = filenameMatch[1];
                const contentTypeMatch = headersText.match(/Content-Type: (.+)/);
                const contentType = contentTypeMatch ? contentTypeMatch[1] : "application/octet-stream";

                // Convert file content to a Blob
                const blob = new Blob([content], { type: contentType });
                files.push({ filename, blob });
            }
        }
    }
    return files;
};

const Home = () => {
    const navigate = useNavigate();
    const fileInput = useRef(null);
    const [loggedIn, setLoggedIn] = useState(null);
    const [file, setFile] = useState([]);
    const [zipName, setZipName] = useState("");
    const [fileSize, setFileSize] = useState(0);
    const [downloadedFiles, setDownloadedFiles] = useState([]);
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
                <button onClick={handleFileDownload}>Download</button>
            </Container>
        </div>
    );
};

export default Home;