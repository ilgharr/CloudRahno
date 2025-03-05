import { useNavigate } from "react-router-dom";
import React, { useEffect, useState, useRef } from "react";
import { Container } from "react-bootstrap";
import CheckSession from "./CheckSession";
import HomeNavbar from "./HomeNavbar";
import UploadCloud from "../assets/cloud_upload.png";
import JSZip from "jszip";

const handleFileUpload = async (file) => {
    if (!file || file.length === 0) {
//        alert("No file selected.");
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
//            alert("Files uploaded successfully!");
        } else {
            const errorMessage = await response.text();
            console.error("Error:", errorMessage);
//            alert("An error occurred while uploading the file(s).");
        }
    } catch (error) {
        console.error("Error during file upload:", error);
//        alert("An error occurred. Please try again.");
    }
};

const handleFileDownload = async () => {
    try {
        const response = await fetch(`/download`, { method: "GET", credentials: "include" });
        if (response.ok) {
            console.log("Request successful:", response.status);
        } else {
            console.error("Request failed with status:", response.status);
        }
    } catch (e) {
        console.error("Error communicating with backend:", e);
    }
}

// removes .zip extension, this is to prevent fileName.zip.zip
const removeZipExtension = (input) => {
  return input.endsWith(".zip") ? input.slice(0, -4) : input;
}

// given an array of files, returns a single .zip with files inside it
const createZip = async (files, fileName) => {
    const zip = new JSZip();
    files.forEach((file) => {zip.file(file.name, file);});
    const zipBlob = await zip.generateAsync({ type: "blob" });
    return new File([zipBlob], removeZipExtension(fileName)+".zip", { type: "application/zip" });
}

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

//    const handleZipSubmit = async () => {
//        try {
//            const zipFile = await createZip(file, zipName);
//            await handleFileUpload([zipFile]);
//            alert("ZIP file uploaded successfully!");
//        } catch (error) {
//            console.error("Error during zip creation or upload:", error);
//        } finally {
//            setFile([]);
//            setFileSize(0);
//        }
//    };


/*
<p className="or">Or</p>
<Container className="zip-submission">
     <p>Submit as a single .zip file</p>
     <Container>
         <input
             type="text"
             value={zipName}
             onChange={(e) => setZipName(e.target.value)}
             placeholder="Enter file name"
         />
         <button onClick={handleZipSubmit}>Submit Zip</button>
     </Container>
*/
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