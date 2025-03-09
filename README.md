# Cloud-Based File Storage Service

## Project Purpose

This project provides a simple, secure, and user-friendly web-based file storage platform. The goal is to allow users to easily upload and retrieve files online, removing the need for local device storage.

The idea is perfect for non-technical users, like my mom, who would like to store her photos in the cloud to free up space on her phone while ensuring easy access and organization. By leveraging cloud services, the platform guarantees secure and scalable storage for all files.

To simplify access without incurring extra costs or legal complexities, account creation is not allowed for general users. Special usernames and passwords can be provided (e.g., to recruiters) for testing purposes.

---

## Features

### Secure Cloud-Based Storage
- Store and retrieve files in the cloud with high reliability and security.
- Files are safely stored using **Amazon S3** to ensure durability and accessibility.

### Simple and Responsive Design
- Easy-to-use web interface built with **React**.
- Accessible on any device, including desktops, tablets, and smartphones, without requiring additional app installations.

### Organized File Management
- Upload, download, and delete files or folders through a simple, intuitive web interface.

---

## Technology Overview

This service leverages modern technologies, including **Java Spring Boot**, **React**, and **AWS**, to ensure reliability, scalability, and security:

- **Java Spring Boot**: Implements the backend REST APIs to handle file operations, user management, and integration with AWS services.
- **React**: A dynamic and responsive frontend framework used to build the user-friendly web interface.
- **AWS CodeBuild and CodePipeline** CI/CD Automation. Source -> Build -> Deploy -> Run
- **Amazon Cognito**: Handles user authentication, including user sign-up, sign-in, and secure access tokens.
- **Amazon S3**: Durable and scalable storage for user files.
- **AWS EC2**: Hosts the backend APIs built with **Java Spring Boot** as well as the **React** frontend.
- **Route 53**: Used to manage the platform's custom domain and route traffic to the hosted EC2 instance.

---

## How to Use

1. **Login**:
   - Use the provided username and password to log in through the secure web interface.

2. **Upload Files**:
   - Drag and drop files into the upload section or use the upload button to add them to your storage.

3. **Download Files**:
   - Select files from your storage and click the download button to retrieve them.

4. **Delete Files**:
   - Manage your storage by selecting unnecessary files or folders and deleting them.

---

## Future Enhancements

This platform has exciting possibilities for future upgrades:

1. **File Sharing**:
   - Add the ability to generate and share **S3 URLs**, letting users share their stored files with others securely.

2. **Mobile-Focused Design**:
   - Extend the current website with a mobile-friendly version or dedicated app for better on-the-go usability.

3. **Enhanced Scalability**:
   - Introduce container orchestration tools (e.g., Kubernetes) to support higher loads as the numbers of users and files grow.

4. **Command-Line Interface (CLI)**:
   - Develop a CLI tool for advanced users to upload, download, and delete files directly from the terminal. This will provide power users (like developers) with a fast and efficient alternative to the web interface.
---

## Summary

This project delivers a simple web-based file storage platform focused on ease-of-use, security, and reliability. By storing files securely on **AWS S3** and hosting APIs on **EC2**, the platform ensures performance and scalability.

Designed with non-technical users in mind, this platform helps users keep their data safe and organized while freeing up valuable local storage. Special credentials allow external testers, such as recruiters, to explore the website functionality with ease.