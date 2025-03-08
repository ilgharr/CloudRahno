package org.ilghar.controller;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    public Integer getNumberOfFiles(String user_id) {
        // gets all object names, not the objects themselves
        ListObjectsV2Result result = s3_client.listObjectsV2(Secrets.BUCKET_NAME, user_id + "/");
        // gets a list containing information for each object, given the above list name
        List<S3ObjectSummary> objects = result.getObjectSummaries();
        // the length of the above list, AKA the number of objects user has
        return objects.size();
    }

    // responds with the total number of objects the current user has in storage
    @GetMapping("/max-count")
    public ResponseEntity<String> sendObjCount(@RequestParam(value = "user_id", required = true) String user_id){
        try {
            String response = String.valueOf(getNumberOfFiles(user_id));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing request: " + e.getMessage());
        }
    }

//    @GetMapping("/get-obj-count")
//    public ResponseEntity<String> sendObjCount(@RequestParam(name = "user_id", required = true) String user_id) {
//        return ResponseEntity.ok(String.valueOf(getNumberOfFiles(user_id)));
//    }

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") List<MultipartFile> files,
                                                   @CookieValue(value = "refresh_token", required = true) String refresh_token) {

        ResponseEntity<?> session_response = Utility.validateUserSession(refresh_token);
        if(session_response != null){
            return (ResponseEntity<String>) session_response;
        }

        String user_id = Utility.extractUserId(Utility.getIdToken(refresh_token));

        if (files == null || files.isEmpty()) {
            System.out.println("No files were uploaded.");
            return ResponseEntity.badRequest().body("No files were uploaded.");
        } else {
            System.out.println(files.size() + " files received for upload.");
        }

        StringBuilder result_message = new StringBuilder();

        try {
            for (MultipartFile file : files) {
                if (!file.isEmpty() && file.getSize() <= MAX_FILE_SIZE) {
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
    
}
