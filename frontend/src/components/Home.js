import { useLocation, useNavigate} from "react-router-dom";
import React, {useEffect, useState, useRef} from 'react';
import { Navbar, Container } from 'react-bootstrap';
import CheckSession from './CheckSession'
import HomeNavbar from './HomeNavbar'
import UploadCloud from '../assets/cloud_upload.png'

const validateFile = (file, allowedTypes, maxSize) => {
    if(!file){
        alert("No file selected!");
        return false;
    }

    if(!allowedTypes.includes(file.type)){
        alert("Invalid file type. Allowed types: " + allowedTypes.join(", "));
        return false;
    }

    if (file.size > maxSize){
        alert("File size exceeds the limit of" + maxSize / (1024 * 1024) + "MB.");
        return false
    }

    return true;
}

const Home = () => {
    const navigate = useNavigate();
    const fileInput = useRef(null);
    const [loggedIn, setLoggedIn] = useState(null);
    const [dragging, setDragging] = useState(false);
    const [file, setFile] = useState(null);
    const [fileName, setFileName] = useState("")
    const allowedFileSize = 20 * 1024 * 1024;
    const allowedTypes = [
        "application/zip", // ZIP
        "application/x-7z-compressed", // 7Z
        "application/x-rar-compressed", // RAR
        "application/gzip", // GZIP
        "application/x-tar", // TAR
        "application/pdf", // PDF
        "application/vnd.ms-powerpoint", // PPT
        "application/vnd.openxmlformats-officedocument.presentationml.presentation", // PPTX
        "application/vnd.ms-excel", // XLS
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // XLSX
        "application/vnd.oasis.opendocument.spreadsheet", // ODS
        "text/csv", // CSV
        "application/msword", // DOC
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // DOCX
        "text/plain", // TXT
        "text/html", // HTML
        "text/css", // CSS
        "application/xml", // XML
        "application/json", // JSON
        "application/javascript", // JavaScript files
        "video/mp4", // MP4
        "video/mpeg", // MPEG
        "audio/mpeg", // MP3
        "image/svg+xml", // SVG
        "image/jpeg", // JPEG, JPG
        "image/png", // PNG
        "image/gif" // GIF
    ];

    CheckSession(setLoggedIn);

//    useEffect(() => {
//        if (loggedIn === "false") {
//            navigate("/");
//        }
//    }, [loggedIn, navigate]);

    const handleClick = () => {
        fileInput.current.click();
    };

    const handleFileChange = (event) => {
        const selectedFile = event.target.files[0];
        if(!validateFile(selectedFile, allowedTypes, allowedFileSize)){return;}
        setFile(selectedFile);
    };

    const handleDragOver = (event) => {
        event.preventDefault();
        setDragging(true);
    };

    const handleDragLeave = (event) => {
        event.preventDefault();
        setDragging(false);
    }

    const handleDrop = (event) => {
        const selectedFile = event.target.files[0];
        if(!validateFile(selectedFile, allowedTypes, allowedFileSize)){return;}
        setFile(selectedFile);
    };

    const handleNameChange = (event) => {
        setFileName(event.target.value);
    }

    return (
        <div>
            <HomeNavbar/>
            <Container className="home-main-container">
                <Container className="upload-file"
                    onClick={handleClick}
                    onDragOver={handleDragOver}
                    onDragLeave={handleDragLeave}
                    onDrop={handleDrop}
                >
                    <p>Drag and Drop a File Here Or Click to Select a File</p>

                    <img src={UploadCloud} alt="Upload File Image" className="upload-file-image" />

                    <input
                        type="file"
                        ref={fileInput}
                        style={{ display: 'none' }}
                        onChange={handleFileChange}
                    />

                </Container>
                {file ? (
                    <Container className="selected-file">
                        <input className="change-file-name"
                            type="text"
                            value={fileName}
                            onChange={handleNameChange}
                            placeholder={file.name}
                        />
                    </Container>
                ) : null}
            </Container>
        </div>
    );
};

export default Home;