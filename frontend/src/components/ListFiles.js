import React, { useEffect, useState } from "react";

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

const ListFiles = () => {
    const [fileList, setFileList] = useState([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            const files = await fetchFileList();
            setFileList(files);
            setIsLoading(false);
        };

        fetchData();
    }, []);

    if (isLoading) {
        return null;
    }

    return (
        <div>
            <h3>File List</h3>
            {fileList.length > 0 ? (
                fileList.map((file, index) => (
                    <button
                        key={index}
                        onClick={() => console.log(file)}
                        style={{ margin: "5px", padding: "10px" }}
                    >
                        {file}
                    </button>
                ))
            ) : (
                <p>No files available</p>
            )}
        </div>
    );
};

export default ListFiles;