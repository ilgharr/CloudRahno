import { useNavigate, useLocation } from "react-router-dom";
import ReactDOM from "react-dom/client";
import React, { useEffect, useState, useRef } from "react";
import { Container } from "react-bootstrap";
import CheckSession from "./CheckSession";
import HomeNavbar from "./HomeNavbar";
import ListFiles from "./ListFiles";
import SimplePopup from "./SimplePopup";


const handleFileUpload = async (file, setUploadResponse) => {
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
            setUploadResponse("File(s) successfully uploaded.");
        } else if(response.status == 400) {
            const errorMessage = await response.text();
            console.error("Bad Request:", errorMessage);
            setUploadResponse(`File upload failed: ${errorMessage}`);
        } else if(response.status == 413){
            const errorMessage = await response.text();
            console.error("Bad Request:", errorMessage);
            setUploadResponse(`File upload failed: ${errorMessage}`);
        } else {
            const errorMessage = await response.text();
            console.error(`Unexpected error (status: ${response.status}):`, errorMessage);
            setUploadResponse("Unexpected error occurred.");
        }
    } catch (error) {
        console.error("Error during file upload:", error);
        setUploadResponse("Failed to connect to the server. Please check your internet connection or try again later.");
    }
};

const Home = () => {
    const location = useLocation(); // not used
    const navigate = useNavigate();
    const MAX_SIZE = 10 * 1024 * 1024;

    const [uploadResponse, setUploadResponse] = useState("");   // holds the response of the upload endpoint
    const [showPopup, setShowPopup] = useState(false);          // state of popup visibility
    const [fileSize, setFileSize] = useState(0);                // size of the files user selected
    const [file, setFile] = useState([]);                       // the file that is sent over API
    const fileInput = useRef(null);                             // this is file(s) the user selects from their computer
    const [loggedIn, setLoggedIn] = useState(null);             // tracks logged in session
    const [updateList, setUpdateList] = useState(false);        // causes ListFiles to re render

    CheckSession(setLoggedIn);

    useEffect(() => {
        setUploadResponse(
            "NOTICE!<br />" +
            "All uploaded content will be automatically deleted after 24 hours." +
            "Please be aware that content may be visible to the developer." +
            "This website is intended solely for showcasing development skills."
        );
        setShowPopup(true);
    },[]);

    useEffect(() => {
        if (loggedIn === "false") {
            navigate("/");
        }
    }, [loggedIn, navigate]);

    useEffect(() => {
        if (uploadResponse !== ""){
            setShowPopup(true); // useState
        }
    }, [uploadResponse]);

    const handleClick = () => {
        fileInput.current.click();
    };

    const handleFileChange = (event) => {
        const selectedFiles = Array.from(event.target.files);
        setFile(selectedFiles); // useState
        const size = selectedFiles.reduce((acc, file) => acc + file.size, 0);
        setFileSize(size);
    };

    const handleFileRemove = () => {
        setFile([]);    // useState
        setFileSize(0); // useState
    };

    const handleFileSubmit = async () => {
        await handleFileUpload(file, setUploadResponse);    // useState
        setUpdateList((prev) => !prev);                     // useState
        setFile([]);                                        // useState
        setFileSize(0);                                     // useState
    };

    return (
        <div>
            {showPopup && (
                <SimplePopup
                    message={uploadResponse}
                    onClose={() => {
                        setShowPopup(false);
                        setUploadResponse("");
                    }}
                />
            )}
            <HomeNavbar />
            <Container>
                {file.length < 1 ? (
                    <Container className="upload-file" onClick={handleClick}>
                        <p>
                            <i class="fa fa-upload"></i>&nbsp;
                            Click to Upload Files
                        </p>
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
            <ListFiles updateList={updateList}/>
        </div>
    );
};

export default Home;