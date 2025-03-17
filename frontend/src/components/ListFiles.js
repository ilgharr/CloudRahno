import React, { useEffect, useState } from "react";
import { Container } from "react-bootstrap";
import SimplePopup from "./SimplePopup";

const fetchFileList = async () => {
    try {
        const response = await fetch(`/fetch-file-list`, {
            method: "GET",
            credentials: "include",
        });

        if (response.ok) {
            const result = await response.json();
            return Array.isArray(result) ? result : [];
        } else {
            console.error("Error:", await response.text());
            return [];
        }
    } catch (error) {
        console.error("Error during file check:", error);
        return [];
    }
};

const triggerFileDownload = (blob, fileName) => {
    const fileUrl = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = fileUrl;
    link.download = fileName;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(fileUrl);
};

const downloadFile = async (fileName) => {
    try {
        const response = await fetch(
            `/download-file?fileName=${encodeURIComponent(fileName)}`,
            {
                method: "GET",
                credentials: "include",
            }
        );

        if (response.ok) {
            const blob = await response.blob();
            triggerFileDownload(blob, fileName);
        } else {
            console.error("Failed to download file:", fileName, await response.text());
        }
    } catch (error) {
        console.error("Error during file download:", error);
    }
};

const deleteFile = async (fileName, setPopupMessage) => {
    try {
        const response = await fetch(
        // filename is encoded for safety
        // certain characters can cause issues
        `/delete-file?fileName=${encodeURIComponent(fileName)}`, {
            method: "GET",
            credentials: "include",
        });

        if (response.ok) {
            const result = await response.text();
            setPopupMessage(fileName + " deleted successfully.");
        } else {
            console.error("Error:", await response.text());
            setPopupMessage(fileName + " could not be deleted.");

        }
    } catch (error) {
        console.error("Error during file check:", error);
        setPopupMessage("Failed to connect to the server. Please check your internet connection or try again later.");

    }
};

const ListFiles = ({ updateList }) => {
    const [fileList, setFileList] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [popupMessage, setPopupMessage] = useState("");

    const fetchData = async () => {
        setIsLoading(true);
        const files = await fetchFileList();
        setFileList(files);
        setIsLoading(false);
    };

    useEffect(() => {
        fetchData();
    }, []);

    useEffect(() => {
        fetchData();
    }, [updateList]);

    if (isLoading) {
        return null;
    };

    const handleDownload = async (file) => {
        await downloadFile(file);
    };

    const handleDelete = async (file) => {
        await deleteFile(file, setPopupMessage);
    };

    const handlePopupClose = async () => {
        setPopupMessage("");
        fetchData();
    };

    return (
        <Container className="file-container">
            {popupMessage && (
                <SimplePopup
                    message={popupMessage}
                    onClose={handlePopupClose}
                />
            )}
            <div className="file-list">
                {fileList.length > 0 ? (
                    fileList.map((file, index) => (
                        <div key={index} className="file-item">
                            <span className="file-name" title={file}>
                                {file}
                            </span>
                            <button
                                onClick={() => handleDownload(file)}
                                className="file-button"
                            >
                                Download
                            </button>
                            <button
                                onClick={() => handleDelete(file)}
                                className="file-button"
                            >
                                Delete
                            </button>
                        </div>
                    ))
                ) : (
                    <p>No files available</p>
                )}
            </div>
        </Container>
    );
};

export default ListFiles;