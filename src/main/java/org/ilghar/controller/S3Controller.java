package org.ilghar.controller;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;

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

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") List<MultipartFile> files,
                                                   @CookieValue(value = "refresh_token", required = true) String refresh_token) {

        if(refresh_token == null){
            System.out.println("User attempted access to /upload while logged out");
            HttpHeaders headers = new HttpHeaders();
            headers.set("Location", "/");
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }

        String user_id = extractUserId(getIdToken(refresh_token));

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

    public static Map<String, String> getIdToken(String refresh_token) {
        System.out.println("Requesting ID token with refresh token...");
        RestTemplate rest_template = new RestTemplate();

        MultiValueMap<String, String> request_body = new LinkedMultiValueMap<>();
        request_body.add("grant_type", "refresh_token");
        request_body.add("client_id", Secrets.CLIENT_ID);
        request_body.add("client_secret", Secrets.CLIENT_SECRET);
        request_body.add("refresh_token", refresh_token);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        try {
            ResponseEntity<String> response = rest_template.postForEntity(
                    Secrets.TOKEN_ENDPOINT, new HttpEntity<>(request_body, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("ID token successfully fetched.");
                return new ObjectMapper().readValue(response.getBody(), new TypeReference<>() {
                });
            } else {
                throw new HttpClientErrorException(response.getStatusCode(), "Token exchange failed.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while exchanging code for tokens", e);
        }
    }

    public static String extractUserId(Map<String, String> token) {
        try {
            if (token == null || !token.containsKey("id_token")) {
                throw new IllegalArgumentException("id_token is missing.");
            }
            String id_token = token.get("id_token");

            String[] token_parts = id_token.split("\\.");
            if (token_parts.length != 3) {
                throw new IllegalArgumentException("Invalid id_token format.");
            }

            String payload = new String(Base64.getDecoder().decode(token_parts[1]));

            int sub_start = payload.indexOf("\"sub\":\"") + 7;
            int sub_end = payload.indexOf("\"", sub_start);

            if (sub_start < 7 || sub_end == -1) {
                throw new IllegalArgumentException("sub claim not found in id_token");
            }

            return payload.substring(sub_start, sub_end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
