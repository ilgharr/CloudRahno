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

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") List<MultipartFile> files,
                                                   @CookieValue(value = "refresh_token", required = false) String refresh_token) {

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

    public List<String> getDirectoryFiles(String user_id) {
        return processS3Objects(user_id, S3ObjectSummary::getKey);
    }

    public long getDirectorySize(String user_id) {
        return processS3Objects(user_id, S3ObjectSummary::getSize)
                .stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    private <T> List<T> processS3Objects(String user_id, java.util.function.Function<S3ObjectSummary, T> mapper) {
        List<T> result_list = new ArrayList<>();

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(Secrets.BUCKET_NAME)
                .withPrefix(user_id)
                .withDelimiter("/");

        ListObjectsV2Result result;

        do {
            result = s3_client.listObjectsV2(request);
            for (S3ObjectSummary object_summary : result.getObjectSummaries()) {
                result_list.add(mapper.apply(object_summary)); // Apply the desired operation
            }
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return result_list;
    }

    @PostMapping("/check-test-user")
    public ResponseEntity<Map<String, String>> handleGetFiles(@CookieValue(value = "refresh_token", required = false) String refresh_token) {
        ResponseEntity<?> session_response = Utility.validateUserSession(refresh_token);
        if(session_response != null){
            return (ResponseEntity<Map<String, String>>) session_response;
        }

        String user_id = Utility.extractUserId(Utility.getIdToken(refresh_token));

        if(Objects.equals(user_id, "dc2dd5c8-8081-7054-447b-7dff1cfdcff7")){

            return ResponseEntity.ok(Map.of("isAllowed", "true"));

        }
            return ResponseEntity.ok(Map.of("isAllowed", "false"));
    }

}
