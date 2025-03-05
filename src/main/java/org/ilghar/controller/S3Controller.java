package org.ilghar.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

public class S3Controller {

    final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @PostMapping("/upload")
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") List<MultipartFile> files,
                                                   @CookieValue(value = "refresh_token", required = true) String refresh_token) {

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

                    Map<String, String> presigned_data = generatePresignedUploadUrl(user_id, file_name);
                    String presigned_url = presigned_data.get("presignedUrl");
                    System.out.println("Pre-signed URL generated: " + presigned_url);

                    boolean upload_success = sendFileToS3(file, presigned_url);

                    if (upload_success) {
                        System.out.println("File uploaded successfully to S3: " + file_name);
                        result_message.append("File uploaded successfully to S3: ").append(file_name).append("\n");
                    } else {
                        System.out.println("Failed to upload file to S3: " + file_name);
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

    // sends refresh token to AWS for id token
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

    // extracts user id "sub" field from the id_token
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

    private boolean sendFileToS3(MultipartFile file, String presigned_url) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(30))  // Increased timeout
                    .build();
            System.out.println("Sending file to S3...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(presigned_url))
                    .PUT(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .header("Content-Type", "application/octet-stream")
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response from S3: " + response.body() + ", Status Code: " + response.statusCode());

            return response.statusCode() == 200;
        } catch (Exception e) {
            System.err.println("Error while uploading file to S3: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, String> generatePresignedUploadUrl(String user_id, String file_name) {
        try {
            System.out.println("Generating pre-signed URL for file: " + file_name);

            final String bucket_name = Secrets.BUCKET_NAME;
            final String region = Secrets.BUCKET_REGION;
            final String service = "s3";
            final String access_key = Secrets.ACCESS_KEY;
            final String secret_key = Secrets.SECRET_KEY;

            String object_key = user_id + "/" + file_name;

            String algorithm = "AWS4-HMAC-SHA256";
            String amz_date = getAmzDate();
            String date_stamp = getDateStamp();
            System.out.println("amzDate (request date in UTC): " + amz_date);

            String credential_scope = date_stamp + "/" + region + "/" + service + "/aws4_request";

            String canonical_uri = "/" + object_key;
            String canonical_query_string = buildCanonicalQueryString(algorithm, access_key, credential_scope, amz_date);

            String canonical_headers = "host:" + bucket_name + ".s3." + region + ".amazonaws.com\n";
            String signed_headers = "host";
            String payload_hash = "UNSIGNED-PAYLOAD";

            String canonical_request = buildCanonicalRequest(
                    "PUT",
                    canonical_uri,
                    canonical_query_string,
                    canonical_headers,
                    signed_headers,
                    payload_hash
            );

            String string_to_sign = buildStringToSign(algorithm, amz_date, credential_scope, canonical_request);

            byte[] signing_key = getSignatureKey(secret_key, date_stamp, region, service);
            String signature = hmacSHA256Hex(string_to_sign, signing_key);

            String presigned_url = "https://" + bucket_name + ".s3." + region + ".amazonaws.com/" + object_key +
                    "?" + canonical_query_string + "&X-Amz-Signature=" + signature;

            // Response map
            Map<String, String> response = new HashMap<>();
            response.put("presignedUrl", presigned_url);
            response.put("key", object_key);

            System.out.println("Generated pre-signed URL: " + presigned_url);
            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error generating pre-signed URL", e);
        }
    }

    private String getAmzDate() {
        SimpleDateFormat utc_format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        utc_format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utc_format.format(new Date());
    }

    private String getDateStamp() {
        SimpleDateFormat date_format = new SimpleDateFormat("yyyyMMdd");
        date_format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return date_format.format(new Date());
    }

    private String buildCanonicalQueryString(String algorithm, String access_key, String credential_scope, String amz_date) throws Exception {
        return "X-Amz-Algorithm=" + algorithm +
                "&X-Amz-Credential=" + URLEncoder.encode(access_key + "/" + credential_scope, StandardCharsets.UTF_8) +
                "&X-Amz-Date=" + amz_date +
                "&X-Amz-Expires=900" +
                "&X-Amz-SignedHeaders=host";
    }

    private String buildCanonicalRequest(String http_method, String canonical_uri, String canonical_query_string,
                                         String canonical_headers, String signed_headers, String payload_hash) {
        return http_method + "\n" +
                canonical_uri + "\n" +
                canonical_query_string + "\n" +
                canonical_headers + "\n" +
                signed_headers + "\n" +
                payload_hash;
    }

    private String buildStringToSign(String algorithm, String amz_date, String credential_scope, String canonical_request) throws Exception {
        return algorithm + "\n" +
                amz_date + "\n" +
                credential_scope + "\n" +
                sha256Hex(canonical_request);
    }

    private static String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed_bytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex_string = new StringBuilder();
        for (byte b : hashed_bytes) {
            hex_string.append(String.format("%02x", b));
        }
        return hex_string.toString();
    }

    private static byte[] hmacSHA256(String data, byte[] key) throws Exception {
        String algorithm = "HmacSHA256";
        Mac hmac = Mac.getInstance(algorithm);
        SecretKeySpec secret_key = new SecretKeySpec(key, algorithm);
        hmac.init(secret_key);
        return hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSHA256Hex(String data, byte[] key) throws Exception {
        byte[] hmac = hmacSHA256(data, key);
        StringBuilder hex_string = new StringBuilder();
        for (byte b : hmac) {
            hex_string.append(String.format("%02x", b));
        }
        return hex_string.toString();
    }

    private static byte[] getSignatureKey(String key, String date_stamp, String region_name, String service_name) throws Exception {
        byte[] date = hmacSHA256(date_stamp, ("AWS4" + key).getBytes(StandardCharsets.UTF_8));
        byte[] region = hmacSHA256(region_name, date);
        byte[] service = hmacSHA256(service_name, region);
        return hmacSHA256("aws4_request", service);
    }
}
