package org.ilghar.controller;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.ilghar.Secrets;
import org.ilghar.controller.Utility;


@RestController
public class S3Controller {

    final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private final AmazonS3 s3_client = AmazonS3ClientBuilder.standard()
            .withRegion(Secrets.BUCKET_REGION)
            .withCredentials(new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(Secrets.ACCESS_KEY, Secrets.SECRET_KEY)))
            .build();

    private void uploadFile(String user_id, MultipartFile file) throws IOException {
        String key = user_id + "/" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        s3_client.putObject(Secrets.BUCKET_NAME, key, file.getInputStream(), metadata);
        System.out.println("Uploaded successfully: " + key);
    }


    // all responses handled by frontend
    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") List<MultipartFile> files,
                                                   @CookieValue(value = "refresh_token", required = false) String refresh_token) {

        ResponseEntity<?> session_response = Utility.validateUserSession(refresh_token);
        if(session_response != null){
            return (ResponseEntity<String>) session_response;
        }

        String user_id = Utility.extractUserId(Utility.getIdToken(refresh_token));

        if (!userHasSpace(user_id)) {
            String errorMessage = "Not enough storage space available.";
            System.err.println(errorMessage);
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorMessage);
        }

        if (files == null || files.isEmpty()) {
            System.out.println("No files were uploaded.");
            return ResponseEntity.badRequest().body("No files were uploaded.");
        } else {
            System.out.println(files.size() + " files received for upload.");
        }

        StringBuilder result_message = new StringBuilder();
        try {
            for (MultipartFile file : files) {
                if (true) {
                    String file_name = file.getOriginalFilename();
                    System.out.println("Processing file: " + file_name);

                    try {
                        uploadFile(user_id, file);
                        System.out.println("File uploaded successfully to S3: " + file_name);
                        result_message.append("File uploaded successfully to S3: ").append(file_name).append("\n");
                    } catch (Exception uploadError) {
                        System.err.println("Failed to upload file to S3: " + file_name);
                        result_message.append("Failed to upload file to S3: ").append(file_name).append("\n");
                    }
                } else {
                    System.out.println("Skipped empty or oversized file: " + file.getOriginalFilename());
                    result_message.append("Skipped empty or oversized file: ").append(file.getOriginalFilename()).append("\n");
                }
            }
            System.out.println("File upload process completed.");
            return ResponseEntity.ok(result_message.toString());
        } catch (Exception e) {
            System.err.println("An error occurred during file upload: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file upload.");
        }
    }

    public List<String> getDirectoryFiles(String user_id) {
        List<String> file_names = new ArrayList<>();

        String prefix = user_id.endsWith("/") ? user_id : user_id + "/";

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(Secrets.BUCKET_NAME)
                .withPrefix(prefix)
                .withDelimiter("/");

        ListObjectsV2Result result;

        do {
            result = s3_client.listObjectsV2(request);
            for (S3ObjectSummary object_summary : result.getObjectSummaries()) {
                String full_key = object_summary.getKey();
                if (!full_key.endsWith("/")) {
                    String file_name = full_key.startsWith(prefix) ? full_key.substring(prefix.length()) : full_key;
                    file_names.add(file_name);
                }
            }

            request.setContinuationToken(result.getNextContinuationToken());

        } while (result.isTruncated());

        return file_names;
    }

    @GetMapping("/fetch-file-list")
    public ResponseEntity<List<String>> fetchFileList(@CookieValue(value = "refresh_token", required = false) String refresh_token) {
        ResponseEntity<?> session_response = Utility.validateUserSession(refresh_token);
        if(session_response != null){
            return (ResponseEntity<List<String>>) session_response;
        }
        String user_id = Utility.extractUserId(Utility.getIdToken(refresh_token));
        List<String> file_list = getDirectoryFiles(user_id);
        System.out.println("File list fetched: " + file_list);
        return ResponseEntity.ok(file_list);
    }

    public boolean userHasSpace(String user_id) {
        long total_size = 0;

        String prefix = user_id.endsWith("/") ? user_id : user_id + "/";

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(Secrets.BUCKET_NAME) // Replace with your bucket name
                .withPrefix(prefix);

        ListObjectsV2Result result;

        do {
            result = s3_client.listObjectsV2(request);
            for (S3ObjectSummary object_summary : result.getObjectSummaries()) {
                total_size += object_summary.getSize();

                if (total_size >= 10_737_418_240L) {
                    return false;
                }
            }
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return true;
    }

    @GetMapping("/delete-file")
    public ResponseEntity<String> deleteFile(@CookieValue(value = "refresh_token", required = false) String refresh_token,
                                             @RequestParam(name = "fileName", required = false) String file) {
        ResponseEntity<?> session_response = Utility.validateUserSession(refresh_token);
        if (session_response != null) {
            return (ResponseEntity<String>) session_response;
        }

        String user_id = Utility.extractUserId(Utility.getIdToken(refresh_token));

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File name must be provided.");
        }

        try {
            String object_key = user_id + "/" + file;
            s3_client.deleteObject(Secrets.BUCKET_NAME, object_key);
            System.out.println("Deleting file: " + object_key);
            return ResponseEntity.ok("File deleted successfully: " + object_key);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                System.err.println("File not found: " + file);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found: " + file);
            }
            System.err.println("S3 error while deleting file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("S3 error while deleting file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("An error occurred during file deletion: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while deleting the file: " + file);
        }
    }

    // NOTE spring boot automatically decodes the URI
    @GetMapping("/download-file")
    public ResponseEntity<?> downloadFile(@CookieValue(value = "refresh_token", required = false) String refresh_token,
                                             @RequestParam(name = "fileName", required = false) String file) {
        ResponseEntity<?> session_response = Utility.validateUserSession(refresh_token);
        if (session_response != null) {
            return (ResponseEntity<String>) session_response;
        }

        String user_id = Utility.extractUserId(Utility.getIdToken(refresh_token));

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File name must not be empty.");
        }

        try {
            String object_key = user_id + "/" + file;
            S3Object s3_object = s3_client.getObject(Secrets.BUCKET_NAME, object_key);
            S3ObjectInputStream s3_object_input_stream = s3_object.getObjectContent();
            ByteArrayResource resource = new ByteArrayResource(s3_object_input_stream.readAllBytes());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file + "\"")
                    .body(resource);

        } catch (AmazonS3Exception s3Exception) {
            if (s3Exception.getStatusCode() == 404) {
                System.err.println("File not found in S3: " + file);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found: " + file);
            }
            System.err.println("S3 error while fetching file: " + s3Exception.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving file from S3: " + s3Exception.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error while processing file: " + file);
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("I/O error occurred while processing the file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error in downloadFile: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred while processing the request.");
        }
    }
}
